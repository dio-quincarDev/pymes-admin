package core_pymes.unit;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.invoice.service.InvoiceCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit: InvoiceCalculator ITBMS 0/7/10")
class InvoiceCalculatorItbmsTest {

    private InvoiceCalculator.CalculatedItem calc(BigDecimal qty, BigDecimal precio, BigDecimal descPct, Integer tasa) {
        return InvoiceCalculator.resolve(new InvoiceCalculator.ResolveRequest(
                qty, precio, null,
                null, null, null, descPct, descPct != null, 1, tasa));
    }

    @Test
    void defaultTasa_is7_whenNull() {
        var c = calc(new BigDecimal("10"), new BigDecimal("10"), null, null);
        assertThat(c.itbmsTasa()).isEqualTo(7);
        assertThat(c.itbmsMonto()).isEqualByComparingTo(new BigDecimal("7.00"));
        assertThat(c.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void tasa0_exento_zeroItbms() {
        var c = calc(new BigDecimal("2"), new BigDecimal("50"), null, 0);
        assertThat(c.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(c.itbmsMonto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(c.itbmsTasa()).isEqualTo(0);
    }

    @Test
    void tasa7_calculates7pct() {
        var c = calc(new BigDecimal("10"), new BigDecimal("5.50"), null, 7);
        // 55 * 0.07 = 3.85
        assertThat(c.itbmsMonto()).isEqualByComparingTo(new BigDecimal("3.85"));
    }

    @Test
    void tasa10_calculates10pct() {
        var c = calc(new BigDecimal("6"), new BigDecimal("10"), null, 10);
        // 60 * 0.10 = 6.00
        assertThat(c.itbmsMonto()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void descuento_beforeImpuesto() {
        // qty 10 * 10 =100, descuento 10% => neto 90, 7% => 6.30
        var c = calc(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), 7);
        assertThat(c.discount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(c.subtotal()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(c.itbmsMonto()).isEqualByComparingTo(new BigDecimal("6.30"));
    }

    @Test
    void invalidTasa_throws() {
        assertThatThrownBy(() -> calc(new BigDecimal("1"), new BigDecimal("10"), null, 15))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("ITBMS");
        assertThatThrownBy(() -> calc(new BigDecimal("1"), new BigDecimal("10"), null, 5))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rounding_halfUp_to2decimals() {
        // 3 * 3.333 = 9.999 => neto 9.999, 7% = 0.69993 => 0.70
        var c = calc(new BigDecimal("3"), new BigDecimal("3.333"), null, 7);
        assertThat(c.itbmsMonto()).isEqualByComparingTo(new BigDecimal("0.70"));
    }

    @Test
    void mixTotals_helper() {
        var a = calc(new BigDecimal("2"), new BigDecimal("50"), null, 0); // exento 100
        var b = calc(new BigDecimal("1"), new BigDecimal("100"), null, 7); // gravado 100 + 7
        var c = calc(new BigDecimal("1"), new BigDecimal("100"), null, 10); // gravado 100 +10
        var list = java.util.List.of(a, b, c);
        assertThat(InvoiceCalculator.sumItbms(list)).isEqualByComparingTo(new BigDecimal("17.00"));
        var exento = list.stream().filter(x -> x.itbmsTasa() == 0).map(InvoiceCalculator.CalculatedItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var gravado = list.stream().filter(x -> x.itbmsTasa() != 0).map(InvoiceCalculator.CalculatedItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(exento).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(gravado).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
