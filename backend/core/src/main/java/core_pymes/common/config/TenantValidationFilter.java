package core_pymes.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class TenantValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String headerTenantId = request.getHeader("X-Tenant-Id");
        String paramTenantId = request.getParameter("tenantId");

        if (headerTenantId != null && paramTenantId != null && !headerTenantId.equals(paramTenantId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Tenant ID mismatch\",\"status\":403}");
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }
}