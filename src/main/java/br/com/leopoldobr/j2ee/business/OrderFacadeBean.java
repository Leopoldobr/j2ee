package br.com.leopoldobr.j2ee.business;

import br.com.leopoldobr.j2ee.domain.*;
import br.com.leopoldobr.j2ee.dto.*;
import br.com.leopoldobr.j2ee.integration.*;
import jakarta.ejb.*;
import java.util.*;
import java.util.stream.Collectors;

/** Session Facade: cada chamada pública é uma transação gerenciada pelo contêiner. */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderFacadeBean implements OrderFacade {
    @EJB private OrderDao orders;
    @EJB private ProductDao products;
    @EJB private OrderAssembler assembler;
    @EJB private OrderPublisher publisher;

    @Override public OrderDetailsDto create(CreateOrderCommand command) {
        if (command == null) throw new InvalidOrderException("Pedido obrigatório.");
        Set<Long> ids = command.lines().stream().map(CreateOrderCommand.Line::productId).collect(Collectors.toSet());
        Map<Long, Product> catalog = products.findByIds(ids);
        Order order = new Order(command.customer());
        for (CreateOrderCommand.Line line : command.lines()) {
            Product product = catalog.get(line.productId());
            if (product == null) throw new InvalidOrderException("Produto não encontrado: " + line.productId());
            order.addLine(product, line.quantity()); // O preço vem do servidor.
        }
        orders.save(order);
        publisher.publish(order.getId()); // Participa da mesma transação JTA.
        return assembler.details(order); // A entidade permanece dentro do negócio.
    }

    @Override public OrderDetailsDto details(String id) {
        return assembler.details(orders.find(id).orElseThrow(OrderNotFoundException::new));
    }

    @Override public List<OrderSummaryDto> recent(int limit) { return orders.findRecent(limit); }

    @Override public List<ProductDto> products() {
        return products.findAll().stream().map(product ->
                new ProductDto(product.getId(), product.getName(), product.getPrice())).toList();
    }

    @Override public void replay(String id) {
        orders.find(id).orElseThrow(OrderNotFoundException::new);
        publisher.publish(id);
    }
}
