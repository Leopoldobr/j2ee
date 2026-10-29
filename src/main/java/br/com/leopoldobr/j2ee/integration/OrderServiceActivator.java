package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.business.OrderProcessingService;
import jakarta.ejb.*;
import jakarta.jms.*;
import java.util.UUID;

/** Service Activator: o MDB adapta a mensagem e delega o caso de uso ao negócio. */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/OrderEvents"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderServiceActivator implements MessageListener {
    @EJB private OrderProcessingService service;

    @Override public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage text)) throw new EJBException("Esperada mensagem de texto.");
            String id = text.getText();
            if (id == null || !UUID.fromString(id).toString().equals(id)) throw new EJBException("ID inválido.");
            service.process(id);
        } catch (JMSException | IllegalArgumentException exception) {
            // Não engolir a falha: rollback permite redelivery; poison messages seguem para DLQ.
            throw new EJBException("Falha ao interpretar evento de pedido.", exception);
        }
    }
}
