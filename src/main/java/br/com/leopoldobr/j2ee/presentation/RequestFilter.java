package br.com.leopoldobr.j2ee.presentation;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Intercepting Filter: políticas transversais executadas antes e depois do controlador. */
@WebFilter(urlPatterns = "/app/*")
public class RequestFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse reply = (HttpServletResponse) response;
        http.setCharacterEncoding("UTF-8");
        reply.setCharacterEncoding("UTF-8");
        String requestId = UUID.randomUUID().toString();
        http.setAttribute("requestId", requestId);
        reply.setHeader("X-Request-Id", requestId);
        reply.setHeader("X-Content-Type-Options", "nosniff");
        reply.setHeader("Cache-Control", "no-store");
        reply.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        HttpSession session = http.getSession();
        String token;
        synchronized (session) {
            token = (String) session.getAttribute("csrfToken");
            if (token == null) {
                byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute("csrfToken", token);
            }
        }
        if ("POST".equals(http.getMethod()) && !validToken(token, http.getParameter("csrf"))) {
            reply.sendError(403, "Token CSRF inválido.");
            return;
        }
        long start = System.nanoTime();
        try { chain.doFilter(http, reply); }
        finally {
            http.getServletContext().log("request=" + requestId + " status=" + reply.getStatus()
                    + " durationMs=" + (System.nanoTime() - start) / 1_000_000);
        }
    }

    static boolean validToken(String expected, String submitted) {
        return submitted != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
