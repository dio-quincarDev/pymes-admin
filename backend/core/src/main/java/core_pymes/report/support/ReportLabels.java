package core_pymes.report.support;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class ReportLabels {

    private static final Locale ES = Locale.forLanguageTag("es");

    private ReportLabels() {
    }

    /** "2026-09" -> "Septiembre 2026". */
    public static String periodo(String periodo) {
        YearMonth ym = YearMonth.parse(periodo);
        return capitalizado(ym.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + ym.getYear();
    }

    /** "T202609" -> "Julio – Septiembre 2026" (o cruza el año). */
    public static String trimestre(String periodo) {
        YearMonth fin = trimestreFin(periodo);
        YearMonth inicio = fin.minusMonths(2);
        String desde = capitalizado(inicio.getMonth().getDisplayName(TextStyle.FULL, ES));
        String hasta = capitalizado(fin.getMonth().getDisplayName(TextStyle.FULL, ES));
        if (inicio.getYear() == fin.getYear()) {
            return desde + " – " + hasta + " " + fin.getYear();
        }
        return desde + " " + inicio.getYear() + " – " + hasta + " " + fin.getYear();
    }

    /** "T202609" -> YearMonth 2026-09. */
    public static YearMonth trimestreFin(String periodo) {
        return YearMonth.parse(periodo.substring(1), DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /** "2026-07" -> "Jul 2026". */
    public static String mesCorto(String periodo) {
        YearMonth ym = YearMonth.parse(periodo);
        String mes = ym.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return capitalizado(mes) + " " + ym.getYear();
    }

    private static String capitalizado(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
