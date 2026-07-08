package core_pymes.product.mapper;

import core_pymes.invoice.domain.Proveedor;
import core_pymes.product.domain.Presentacion;
import core_pymes.product.domain.Producto;
import core_pymes.product.dto.PresentacionResponse;
import core_pymes.product.dto.ProductoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductoMapper {

    default ProductoResponse toResponse(Producto p, List<PresentacionResponse> presentaciones) {
        return new ProductoResponse(p.getId(), p.getTenantId(), p.getName(), p.getSku(),
                p.getCategory(), p.getBaseUnit(), p.getImageUrl(), p.getIsActive(),
                p.getCreatedAt(), p.getUpdatedAt(), presentaciones,
                p.getLastUnitPrice(), p.getTotalInvestment(), p.getLastPurchaseDate(),
                p.getMinQuantity(), p.getMaxQuantity(),
                p.getProviderId(), null);
    }

    default ProductoResponse toResponse(Producto p, List<PresentacionResponse> presentaciones, Proveedor proveedor) {
        return new ProductoResponse(p.getId(), p.getTenantId(), p.getName(), p.getSku(),
                p.getCategory(), p.getBaseUnit(), p.getImageUrl(), p.getIsActive(),
                p.getCreatedAt(), p.getUpdatedAt(), presentaciones,
                p.getLastUnitPrice(), p.getTotalInvestment(), p.getLastPurchaseDate(),
                p.getMinQuantity(), p.getMaxQuantity(),
                p.getProviderId(), proveedor != null ? proveedor.getName() : null);
    }

    PresentacionResponse toResponse(Presentacion presentacion);

    List<PresentacionResponse> toResponseList(List<Presentacion> presentaciones);
}
