package br.com.leopoldobr.j2ee.business;

import br.com.leopoldobr.j2ee.domain.*;
import br.com.leopoldobr.j2ee.dto.*;
import br.com.leopoldobr.j2ee.integration.ProductDao;
import jakarta.ejb.*;
import java.util.*;
import java.util.stream.Collectors;

/** Value Object Assembler: compõe dados do pedido JPA e do catálogo JDBC. */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class OrderAssembler {
    @EJB private ProductDao products;

    public OrderDetailsDto details(Order order) {
        Set<Long> ids = order.getLines().stream().map(OrderLine::getProductId).collect(Collectors.toSet());
        Map<Long, Product> catalog = products.findByIds(ids); // Uma consulta em lote; evita N+1.
        List<OrderLineDto> lines = order.getLines().stream().map(line -> {
            Product current = catalog.get(line.getProductId());
            return new OrderLineDto(line.getProductId(), line.getNameAtPurchase(),
                    current == null ? "Produto fora do catálogo" : current.getName(),
                    line.getQuantity(), line.getUnitPrice(), line.getTotal());
        }).toList();
        return new OrderDetailsDto(order.getId(), order.getCustomer(), order.getStatus(), order.getCreatedAt(),
                lines, order.getTotal(), order.getProcessingCount());
    }
}
