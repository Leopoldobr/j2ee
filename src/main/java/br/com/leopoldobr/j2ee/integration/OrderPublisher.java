package br.com.leopoldobr.j2ee.integration;

import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.jms.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
@JMSDestinationDefinition(name = "java:/jms/queue/OrderEvents", interfaceName = "jakarta.jms.Queue",
        destinationName = "OrderEvents")
public class OrderPublisher {
    @Inject private JMSContext context;
    @Resource(lookup = "java:/jms/queue/OrderEvents") private Queue queue;

    public void publish(String orderId) {
        // Mensagem textual: não desserializa objetos Java vindos da fila.
        context.createProducer().setDeliveryMode(DeliveryMode.PERSISTENT).send(queue, orderId);
    }
}
