package core_pymes.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class InvoiceCalculator {

    private InvoiceCalculator() {}

    public record CalculatedItem(
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal subtotal,
            BigDecimal cantidadPresentacionOriginal,
            BigDecimal valorPresentacionOriginal,
            BigDecimal precioUnitarioInputOriginal,
            BigDecimal descuentoInputOriginal,
            Boolean descuentoEsPorcentajeOriginal
    ) {}

    public record ResolveRequest(
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal cantidadPresentacion,
            BigDecimal valorPresentacion,
            BigDecimal precioUnitarioInput,
            BigDecimal descuentoInput,
            Boolean descuentoEsPorcentaje,
            int conversionFactor
    ) {}

    public static CalculatedItem resolve(ResolveRequest req) {
        BigDecimal conversion = BigDecimal.valueOf(req.conversionFactor());
        BigDecimal quantity = null;
        BigDecimal unitPrice = null;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal subtotal = null;

        // Preserve original inputs for audit
        BigDecimal cantPresOrig = req.cantidadPresentacion();
        BigDecimal valPresOrig = req.valorPresentacion();
        BigDecimal puInputOrig = req.precioUnitarioInput();
        BigDecimal descInputOrig = req.descuentoInput();
        Boolean descEsPctOrig = req.descuentoEsPorcentaje();

        // 1) Resolve quantity (base units)
        if (req.cantidadPresentacion() != null) {
            quantity = req.cantidadPresentacion().multiply(conversion);
        } else if (req.cantidad() != null) {
            quantity = req.cantidad();
        }

        // 2) Resolve unitPrice (per base unit)
        if (req.valorPresentacion() != null && conversion.compareTo(BigDecimal.ZERO) > 0) {
            unitPrice = req.valorPresentacion().divide(conversion, 6, RoundingMode.HALF_UP);
        } else if (req.precioUnitarioInput() != null) {
            unitPrice = req.precioUnitarioInput();
        } else if (req.precioUnitario() != null) {
            unitPrice = req.precioUnitario();
        }

        // 3) Resolve subtotal if directly derivable
        if (req.cantidadPresentacion() != null && req.valorPresentacion() != null) {
            subtotal = req.cantidadPresentacion().multiply(req.valorPresentacion());
        } else if (req.cantidad() != null && req.precioUnitario() != null) {
            subtotal = req.cantidad().multiply(req.precioUnitario());
        }

        // 4) Validate: need at least 2 independent inputs to resolve
        List<String> provided = new ArrayList<>();
        if (req.cantidadPresentacion() != null || req.cantidad() != null) provided.add("cantidad");
        if (req.valorPresentacion() != null || req.precioUnitarioInput() != null || req.precioUnitario() != null) provided.add("precio");
        if (req.cantidadPresentacion() != null && req.valorPresentacion() != null) provided.add("subtotal");

        if (provided.size() < 2 && (quantity == null || unitPrice == null)) {
            throw new IllegalArgumentException("Se requieren al menos 2 inputs independientes (cantidad + precio/valor/subtotal)");
        }

        // 5) Derive missing quantity/unitPrice from subtotal if available
        if (quantity == null) {
            if (subtotal != null && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
                quantity = subtotal.divide(unitPrice, 6, RoundingMode.HALF_UP);
            } else {
                throw new IllegalArgumentException("No se puede resolver cantidad: faltan datos");
            }
        }
        if (unitPrice == null) {
            if (subtotal != null && quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                unitPrice = subtotal.divide(quantity, 6, RoundingMode.HALF_UP);
            } else {
                throw new IllegalArgumentException("No se puede resolver precio unitario: faltan datos");
            }
        }

        // 6) Resolve discount
        if (req.descuentoInput() != null) {
            if (Boolean.TRUE.equals(req.descuentoEsPorcentaje())) {
                BigDecimal base = quantity.multiply(unitPrice);
                discount = base.multiply(req.descuentoInput()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            } else {
                discount = req.descuentoInput();
            }
        } else if (req.descuento() != null) {
            discount = req.descuento();
        }

        // 7) Final subtotal (net)
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal netSubtotal = gross.subtract(discount);

        return new CalculatedItem(
                quantity, unitPrice, discount, netSubtotal,
                cantPresOrig, valPresOrig, puInputOrig, descInputOrig, descEsPctOrig
        );
    }
}
