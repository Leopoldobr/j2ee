package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.domain.*;
import br.com.leopoldobr.j2ee.dto.*;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/app/*")
public class FrontController extends HttpServlet {
    @Inject private OrderDelegate delegate;
    @Inject private LegacyOrderDelegate legacy;
    @Inject private OrderSearchModel search;
    @Inject private ViewDispatcher dispatcher;

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String path = Objects.toString(request.getPathInfo(), "/");
            switch (path) {
                case "/", "/orders" -> {
                    int page = Integer.parseInt(Objects.toString(request.getParameter("page"), "0"));
                    request.setAttribute("ordersPage", search.page("true".equals(request.getParameter("refresh")), page));
                    dispatcher.show(ViewDispatcher.View.ORDERS, request, response);
                }
                case "/new" -> {
                    request.setAttribute("products", delegate.products());
                    dispatcher.show(ViewDispatcher.View.NEW, request, response);
                }
                case "/order" -> {
                    request.setAttribute("order", delegate.details(orderId(request)));
                    dispatcher.show(ViewDispatcher.View.ORDER, request, response);
                }
                // Dispatcher View: o controlador NÃO carrega o catálogo.
                case "/catalog" -> dispatcher.show(ViewDispatcher.View.CATALOG, request, response);
                case "/locator" -> {
                    request.setAttribute("recent", legacy.recent());
                    dispatcher.show(ViewDispatcher.View.LOCATOR, request, response);
                }
                default -> response.sendError(404);
            }
        } catch (InvalidOrderException | IllegalArgumentException exception) {
            error(400, exception.getMessage(), request, response);
        } catch (OrderNotFoundException exception) {
            error(404, exception.getMessage(), request, response);
        } catch (ServiceUnavailableException exception) {
            getServletContext().log("Falha de serviço. request=" + request.getAttribute("requestId"), exception);
            error(503, exception.getMessage(), request, response);
        }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String path = Objects.toString(request.getPathInfo(), "/");
            String id;
            if ("/order".equals(path)) {
                OrderDetailsDto order = delegate.create(parseCommand(request));
                search.invalidate();
                id = order.getId();
            } else if ("/replay".equals(path)) {
                id = orderId(request);
                delegate.replay(id);
            } else {
                response.sendError(404); return;
            }
            // Post/Redirect/Get: atualizar a página não reenvia o formulário.
            response.setStatus(303);
            response.setHeader("Location", response.encodeRedirectURL(request.getContextPath() + "/app/order?id=" + id));
        } catch (InvalidOrderException | IllegalArgumentException exception) {
            error(400, exception.getMessage(), request, response);
        } catch (OrderNotFoundException exception) {
            error(404, exception.getMessage(), request, response);
        } catch (ServiceUnavailableException exception) {
            getServletContext().log("Falha de serviço. request=" + request.getAttribute("requestId"), exception);
            error(503, exception.getMessage(), request, response);
        }
    }

    static CreateOrderCommand parseCommand(HttpServletRequest request) {
        String[] ids = request.getParameterValues("productId");
        String[] quantities = request.getParameterValues("quantity");
        if (ids == null || quantities == null || ids.length != quantities.length || ids.length > 20) {
            throw new InvalidOrderException("Produtos e quantidades inválidos.");
        }
        List<CreateOrderCommand.Line> lines = new ArrayList<>();
        for (int index = 0; index < ids.length; index++) {
            long productId = Long.parseLong(ids[index]);
            int quantity = Integer.parseInt(quantities[index]);
            if (productId <= 0 || quantity < 0 || quantity > 100) throw new InvalidOrderException("Produto ou quantidade inválidos.");
            if (quantity > 0) lines.add(new CreateOrderCommand.Line(productId, quantity));
        }
        return new CreateOrderCommand(request.getParameter("customer"), lines);
    }

    private String orderId(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null || !UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("ID inválido.");
        return id;
    }

    private void error(int status, String message, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        dispatcher.show(ViewDispatcher.View.ERROR, request, response);
    }
}
