package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.dto.OrderSummaryDto;
import br.com.leopoldobr.j2ee.integration.ServiceLocator;
import jakarta.ejb.EJBException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;

@RequestScoped
public class LegacyOrderDelegate {
    @Inject private ServiceLocator locator;
    public List<OrderSummaryDto> recent() {
        try { return locator.orderFacade().recent(5); }
        catch (EJBException | IllegalStateException exception) {
            locator.invalidate();
            throw new ServiceUnavailableException(exception);
        }
    }
}
