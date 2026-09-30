package core_pymes.report;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Controller: Reporte mensual - edge cases")
class MonthlyReportControllerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    core_pymes.report.config.MonthlyReportScheduler scheduler;

    @Test
    @DisplayName("Sin header X-User-Role → 403")
    void sinRol() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .param("period", "2026-06"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(scheduler);
    }

    @Test
    @DisplayName("Rol USER no autorizado → 403")
    void rolUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "USER")
                        .param("period", "2026-06"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(scheduler);
    }

    @Test
    @DisplayName("Rol OWNER autorizado → 200")
    void rolOwner() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-06"))
                .andExpect(status().isOk());
        verify(scheduler).runPeriod(eq("2026-06"), eq(true));
    }

    @Test
    @DisplayName("Rol ADMIN autorizado → 200")
    void rolAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "ADMIN")
                        .param("period", "2026-06"))
                .andExpect(status().isOk());
        verify(scheduler).runPeriod(eq("2026-06"), eq(true));
    }

    @Test
    @DisplayName("Period vacío → 400")
    void periodVacio() throws Exception {
        doThrow(new InvalidInputException("Invalid period, expected YYYY-MM: "))
                .when(scheduler).runPeriod(eq(""), anyBoolean());

        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", ""))
                .andExpect(status().isBadRequest());
        verify(scheduler).runPeriod(eq(""), anyBoolean());
    }

    @Test
    @DisplayName("Period formato inválido → 400")
    void periodFormatoInvalido() throws Exception {
        doThrow(new InvalidInputException("Invalid period, expected YYYY-MM: no-es-mes"))
                .when(scheduler).runPeriod(eq("no-es-mes"), anyBoolean());

        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "no-es-mes"))
                .andExpect(status().isBadRequest());
        verify(scheduler).runPeriod(eq("no-es-mes"), anyBoolean());
    }

    @Test
    @DisplayName("Period mes inválido (13) → 400")
    void periodMesInvalido() throws Exception {
        doThrow(new InvalidInputException("Invalid period, expected YYYY-MM: 2026-13"))
                .when(scheduler).runPeriod(eq("2026-13"), anyBoolean());

        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-13"))
                .andExpect(status().isBadRequest());
        verify(scheduler).runPeriod(eq("2026-13"), anyBoolean());
    }

    @Test
    @DisplayName("Period año inválido → 400")
    void periodAnoInvalido() throws Exception {
        doThrow(new InvalidInputException("Invalid period, expected YYYY-MM: abc-06"))
                .when(scheduler).runPeriod(eq("abc-06"), anyBoolean());

        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "abc-06"))
                .andExpect(status().isBadRequest());
        verify(scheduler).runPeriod(eq("abc-06"), anyBoolean());
    }

    @Test
    @DisplayName("dryRun default true → 200")
    void dryRunDefaultTrue() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-06"))
                .andExpect(status().isOk());
        verify(scheduler).runPeriod(eq("2026-06"), eq(true));
    }

    @Test
    @DisplayName("dryRun=false explícito → 200")
    void dryRunFalse() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-06")
                        .param("dryRun", "false"))
                .andExpect(status().isOk());
        verify(scheduler).runPeriod(eq("2026-06"), eq(false));
    }

    @Test
    @DisplayName("Llamadas repetidas → scheduler invocado cada vez")
    void llamadasRepetidas() throws Exception {
        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-06"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/core/reportes/monthly")
                        .header("X-User-Role", "OWNER")
                        .param("period", "2026-06"))
                .andExpect(status().isOk());

        verify(scheduler, org.mockito.Mockito.times(2)).runPeriod(eq("2026-06"), eq(true));
    }
}