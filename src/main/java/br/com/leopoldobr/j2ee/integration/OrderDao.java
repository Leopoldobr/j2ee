package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.domain.Order;
import br.com.leopoldobr.j2ee.dto.OrderSummaryDto;
import jakarta.ejb.Local;
import java.util.*;

@Local
public interface OrderDao {
    void save(Order order);
    Optional<Order> find(String id);
    Optional<Order> findForUpdate(String id);
    List<OrderSummaryDto> findRecent(int limit);
}
