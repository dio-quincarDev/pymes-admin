package core_pymes.report.repository;

import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportDataRepository {

    List<ReportOwner> findOwners();

    ReportData loadMonthData(UUID tenantId, String periodo);

    QuarterlyReportData loadQuarterData(UUID tenantId, YearMonth fin);

    /** true si el trimestre anclado al primer registro del tenant cierra en `fin`. */
    boolean isQuarterDue(UUID tenantId, YearMonth fin);

    /** Reserva el envío. false = ya enviado/en curso/omitido, skip. FAILED se retoma. */
    boolean claim(UUID tenantId, String periodo, String format);

    void updateStatus(UUID tenantId, String periodo, String format, String status, String errorMsg);
}
