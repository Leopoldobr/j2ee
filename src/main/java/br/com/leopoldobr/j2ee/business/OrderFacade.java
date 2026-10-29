package br.com.leopoldobr.j2ee.business;

import br.com.leopoldobr.j2ee.dto.*;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface OrderFacade {
    OrderDetailsDto create(CreateOrderCommand command);
    OrderDetailsDto details(String id);
    List<OrderSummaryDto> recent(int limit);
    List<ProductDto> products();
    void replay(String id);
}
