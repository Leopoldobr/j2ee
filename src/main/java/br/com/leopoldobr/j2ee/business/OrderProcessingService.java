package br.com.leopoldobr.j2ee.business;

import br.com.leopoldobr.j2ee.domain.Order;
import br.com.leopoldobr.j2ee.integration.OrderDao;
import jakarta.ejb.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderProcessingService {
    @EJB private OrderDao orders;

    public void process(String id) {
        // Serializa entregas concorrentes do mesmo pedido em diferentes instâncias do MDB.
        Order order = orders.findForUpdate(id).orElseThrow(() ->
                new IllegalStateException("Mensagem referencia pedido inexistente."));
        order.process(); // Estado já PROCESSED: efeito não é repetido.
        // Dirty checking atualiza o pedido no commit da transação do MDB.
    }
}
