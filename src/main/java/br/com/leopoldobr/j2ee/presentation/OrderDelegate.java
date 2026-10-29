package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.business.OrderFacade;
import br.com.leopoldobr.j2ee.dto.*;
import jakarta.ejb.*;
import jakarta.enterprise.context.RequestScoped;
import java.util.List;
import java.util.function.Supplier;

/** Business Delegate: a apresentação conhece DTOs, sem chamadas JPA/JMS. */
@RequestScoped
public class OrderDelegate {
    @EJB private OrderFacade facade;

    public OrderDetailsDto create(CreateOrderCommand command) { return invoke(() -> facade.create(command)); }
    public OrderDetailsDto details(String id) { return invoke(() -> facade.details(id)); }
    public List<OrderSummaryDto> recent(int limit) { return invoke(() -> facade.recent(limit)); }
    public List<ProductDto> products() { return invoke(() -> facade.products()); }
    public void replay(String id) { invoke(() -> { facade.replay(id); return null; }); }

    private <T> T invoke(Supplier<T> call) {
        try { return call.get(); }
        catch (EJBException exception) { throw new ServiceUnavailableException(exception); }
        // InvalidOrderException e OrderNotFoundException são application exceptions e passam intactas.
        // Não repetir automaticamente create(): uma falha de transporte pode ocorrer após um commit.
    }
}
