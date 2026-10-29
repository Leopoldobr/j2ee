package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.business.OrderFacade;
import jakarta.enterprise.context.ApplicationScoped;
import javax.naming.*;
import java.util.concurrent.ConcurrentHashMap;

/** Variante didática para clientes legados. No fluxo principal usa-se injeção. */
@ApplicationScoped
public class ServiceLocator {
    private static final String ORDER_FACADE =
            "java:module/OrderFacadeBean!br.com.leopoldobr.j2ee.business.OrderFacade";
    private final ConcurrentHashMap<String, Object> proxies = new ConcurrentHashMap<>();

    public OrderFacade orderFacade() {
        return OrderFacade.class.cast(proxies.computeIfAbsent(ORDER_FACADE, this::lookup));
    }

    private Object lookup(String name) {
        try {
            Context context = new InitialContext();
            try { return context.lookup(name); }
            finally { context.close(); }
        } catch (NamingException exception) {
            throw new IllegalStateException("Não foi possível localizar a fachada.", exception);
        }
    }
    public void invalidate() { proxies.clear(); }
}
