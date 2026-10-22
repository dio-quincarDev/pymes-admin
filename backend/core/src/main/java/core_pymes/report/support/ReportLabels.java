package core_pymes.report.support;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public final class ReportLabels {

    private static final Locale ES = Locale.forLanguageTag("es");

    private ReportLabels() {
    }

    /** "2026-09" -> "Septiembre 2026". */
    public static String periodo(String periodo) {
        YearMonth ym = YearMonth.parse(periodo);
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + ym.getYear();
    }
}
