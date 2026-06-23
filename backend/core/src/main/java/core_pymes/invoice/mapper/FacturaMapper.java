package core_pymes.invoice.mapper;

import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.dto.ItemFacturaResponse;
import core_pymes.invoice.dto.FacturaResponse;
import core_pymes.invoice.dto.ProveedorResponse;
import core_pymes.invoice.domain.Proveedor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FacturaMapper {

    default FacturaResponse toResponse(Factura f, List<ItemFacturaResponse> items) {
        return new FacturaResponse(f.getId(), f.getTenantId(), f.getProviderId(),
                f.getProveedor() != null ? f.getProveedor().getName() : null,
                f.getInvoiceNumber(), f.getIssueDate(), f.getType(),
                f.getGlobalDiscount(), f.getPaymentMethod(), f.getStatus(),
                f.getTotal(), items, f.getCreatedAt());
    }

    ItemFacturaResponse toItemResponse(ItemFactura item);

    List<ItemFacturaResponse> toItemResponseList(List<ItemFactura> items);

    ProveedorResponse toProveedorResponse(Proveedor proveedor);

    List<ProveedorResponse> toProveedorResponseList(List<Proveedor> proveedores);
}
