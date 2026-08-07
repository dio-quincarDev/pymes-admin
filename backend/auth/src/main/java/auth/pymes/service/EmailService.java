package auth.pymes.service;

import java.util.Map;

public interface EmailService {
    /**
     * Envía un correo electrónico basado en una plantilla.
     *
     * @param to           Destinatario
     * @param subject      Asunto del correo
     * @param templateName Nombre de la plantilla a usar
     * @param variables    Variables para la plantilla
     */
    void send(String to, String subject, String templateName, Map<String, Object> variables);
}
