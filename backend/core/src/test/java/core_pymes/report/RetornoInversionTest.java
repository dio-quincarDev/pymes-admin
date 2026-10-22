package core_pymes.report;

import core_pymes.analytics.dto.ProyeccionOperativa;
import core_pymes.report.support.RetornoInversion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RetornoInversion: lineas del cierre sin Docker")
class RetornoInversionTest {

    private static final ProyeccionOperativa OP = new ProyeccionOperativa(
            new BigDecimal("7519.00"), new BigDecimal("300.00"),
            new BigDecimal("293.00"), new BigDecimal("366.00"));

    @Test
    @DisplayName("Mes bueno: aparta con ritmo + resto con rango y porcentaje")
    void mesBueno() {
        var lineas = RetornoInversion.armar(
                new BigDecimal("10000"), new BigDecimal("4600"), new BigDecimal("8500"), OP, "USD");

        assertThat(lineas.ausente()).isFalse();
        assertThat(lineas.linea1()).isEqualTo(
                "Te quedaron USD 4,600.00: aparta USD 1,666.67 para el retorno de tu inversión inicial (USD 10,000.00) (ritmo 6 meses).");
        assertThat(lineas.linea2()).isEqualTo(
                "Con USD 2,933.33 te alcanza para operar entre 8 y 10 días (39% del gasto mensual).");
    }

    @Test
    @DisplayName("Margen bajo: protege la caja, sin segunda linea")
    void protegeLaCaja() {
        var lineas = RetornoInversion.armar(
                new BigDecimal("10000"), new BigDecimal("2000"), new BigDecimal("20000"), OP, "USD");

        assertThat(lineas.linea1()).isEqualTo("Este mes no apartes: protege la caja.");
        assertThat(lineas.linea2()).isNull();
    }

    @Test
    @DisplayName("Mes en perdida: protege la caja")
    void mesEnPerdida() {
        var lineas = RetornoInversion.armar(
                new BigDecimal("10000"), new BigDecimal("-500"), new BigDecimal("8000"), OP, "USD");

        assertThat(lineas.linea1()).isEqualTo("Este mes no apartes: protege la caja.");
        assertThat(lineas.linea2()).isNull();
    }

    @Test
    @DisplayName("Sin capital: bloque ausente")
    void sinCapital() {
        assertThat(RetornoInversion.armar(null, new BigDecimal("4600"), new BigDecimal("8500"), OP, "USD")
                .ausente()).isTrue();
        assertThat(RetornoInversion.armar(BigDecimal.ZERO, new BigDecimal("4600"), new BigDecimal("8500"), OP, "USD")
                .ausente()).isTrue();
    }

    @Test
    @DisplayName("Sin gasto en el mes: solo la linea de aparte")
    void sinGasto() {
        var sinOp = new ProyeccionOperativa(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        var lineas = RetornoInversion.armar(
                new BigDecimal("10000"), new BigDecimal("4600"), new BigDecimal("8500"), sinOp, "USD");

        assertThat(lineas.ausente()).isFalse();
        assertThat(lineas.linea2()).isNull();
    }
}
