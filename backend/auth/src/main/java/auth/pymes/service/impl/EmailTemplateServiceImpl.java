package auth.pymes.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl {

    private final SpringTemplateEngine templateEngine;

    /**
     * Renderiza una plantilla Thymeleaf con las variables proporcionadas.
     *
     * @param templateName Nombre de la plantilla (sin extensión .html)
     * @param variables    Mapa de variables para inyectar en la plantilla
     * @return HTML renderizado
     */
    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }
}
