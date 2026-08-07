package core_pymes.venta.repository;

import core_pymes.venta.domain.VentaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VentaRepository extends JpaRepository<VentaDiaria, UUID> {

    List<VentaDiaria> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<VentaDiaria> findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
            UUID tenantId, LocalDate from, LocalDate to);
}
