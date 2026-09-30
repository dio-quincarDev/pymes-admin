package core_pymes.report.repository;

import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportDataRepository {

    List<ReportOwner> findOwners();

    ReportData loadMonthData(UUID tenantId, String periodo);

    /** Reserva el envío (PK + ON CONFLICT DO NOTHING). false = ya existe, skip. */
    boolean claim(UUID tenantId, String periodo, String format);

    void updateStatus(UUID tenantId, String periodo, String format, String status, String errorMsg);
}
