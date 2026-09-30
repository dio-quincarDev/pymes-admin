package core_pymes.report.service.impl;

import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.exception.ReportGenerationException;
import core_pymes.report.service.ReportEmailService;
import core_pymes.report.support.ReportLabels;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportEmailServiceImpl implements ReportEmailService {

    private static final Locale ES = Locale.forLanguageTag("es");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    @Override
    public void sendMonthlyReport(ReportOwner owner, ReportData data, byte[] pdf) {
        String label = ReportLabels.periodo(data.periodo());
        String subject = owner.tenantName() + " — Resumen " + label;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(owner.email());
            helper.setSubject(subject);
            helper.setText(body(owner, data, label), true);
            helper.addAttachment("Resumen-" + data.periodo() + ".pdf",
                    new ByteArrayResource(pdf), "application/pdf");

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new ReportGenerationException(
                    "No se pudo enviar el reporte " + data.periodo() + " a " + owner.email(), e);
        }
    }

    private String body(ReportOwner owner, ReportData data, String label) {
        String linea = "<p><b>" + escape(owner.tenantName()) + " — " + label + ":</b> vendiste "
                + money(owner, data.ingresos()) + ", te quedaron " + money(owner, data.neto()) + ".</p>";
        String parcial = data.esParcial() ? "<p>Sin ventas registradas en el periodo.</p>" : "";
        return linea + parcial + "<p>Adjunto el resumen en PDF.</p><p>— PymeQ</p>";
    }

    private String money(ReportOwner owner, BigDecimal v) {
        return owner.currency() + " " + String.format(ES, "%,.2f", v);
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
