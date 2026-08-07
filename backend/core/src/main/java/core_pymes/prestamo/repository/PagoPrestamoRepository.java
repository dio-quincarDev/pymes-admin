package core_pymes.prestamo.repository;

import core_pymes.prestamo.domain.PagoPrestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagoPrestamoRepository extends JpaRepository<PagoPrestamo, UUID> {

    List<PagoPrestamo> findByLoanIdOrderByPaymentDateAsc(UUID loanId);

    List<PagoPrestamo> findByLoanIdIn(List<UUID> loanIds);
}
