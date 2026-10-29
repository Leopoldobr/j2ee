package br.com.leopoldobr.j2ee.presentation;

import jakarta.enterprise.context.RequestScoped;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

@RequestScoped
public class ViewDispatcher {
    public enum View {
        ORDERS("orders"), ORDER("order"), NEW("new"), CATALOG("catalog"), LOCATOR("locator"), ERROR("error");
        private final String name;
        View(String name) { this.name = name; }
    }
    public void show(View view, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Allowlist de views: nenhum caminho recebido do cliente é encaminhado diretamente.
        request.getRequestDispatcher("/WEB-INF/views/" + view.name + ".jsp").forward(request, response);
    }
}
