package br.com.leopoldobr.j2ee.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    private Product product(long id, String amount) { return new Product(id, "Produto " + id, new BigDecimal(amount)); }

    @Test void preservesMoneyPrecisionAndHistoricalPrices() {
        Order order = new Order("  Leo  ");
        order.addLine(product(1, "0.10"), 3);
        order.addLine(product(2, "0.20"), 2);
        assertEquals(new BigDecimal("0.70"), order.getTotal());
        assertEquals("Leo", order.getCustomer());
        assertEquals(new BigDecimal("0.10"), order.getLines().get(0).getUnitPrice());
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 101})
    void refusesInvalidQuantities(int quantity) {
        Order order = new Order("Leo");
        assertThrows(InvalidOrderException.class, () -> order.addLine(product(1, "1.00"), quantity));
        assertTrue(order.getLines().isEmpty());
    }

    @Test void rootControlsDependentObjects() {
        Order order = new Order("Leo");
        order.addLine(product(1, "10.00"), 1);
        order.addLine(product(2, "2.50"), 2);
        order.changeQuantity(1, 3);
        order.removeLine(2);
        assertEquals(new BigDecimal("30.00"), order.getTotal());
        assertThrows(UnsupportedOperationException.class, () -> order.getLines().clear());
        assertThrows(InvalidOrderException.class, () -> order.removeLine(1));
    }

    @Test void repeatedEventsApplyEffectOnlyOnce() {
        Order order = new Order("Leo");
        order.addLine(product(1, "10.00"), 2);
        assertTrue(order.process());
        assertFalse(order.process());
        assertEquals(1, order.getProcessingCount());
        assertEquals(OrderStatus.PROCESSED, order.getStatus());
        assertThrows(InvalidOrderException.class, () -> order.changeQuantity(1, 3));
        assertThrows(InvalidOrderException.class, () -> order.addLine(product(2, "1.00"), 1));
    }

    @Test void rejectsDuplicateProductsAndEmptyOrder() {
        Order order = new Order("Leo");
        assertThrows(InvalidOrderException.class, order::validateForSubmission);
        assertThrows(InvalidOrderException.class, order::process);
        order.addLine(product(1, "10.00"), 1);
        assertThrows(InvalidOrderException.class, () -> order.addLine(product(1, "20.00"), 2));
        assertEquals(new BigDecimal("10.00"), order.getTotal());
    }

    @Test void limitsAggregateSize() {
        Order order = new Order("Leo");
        for (int index = 1; index <= 20; index++) order.addLine(product(index, "1.00"), 1);
        assertThrows(InvalidOrderException.class, () -> order.addLine(product(21, "1.00"), 1));
    }

    @Test void rejectsBlankAndOversizedCustomer() {
        assertThrows(InvalidOrderException.class, () -> new Order(" "));
        assertThrows(InvalidOrderException.class, () -> new Order(null));
        assertThrows(InvalidOrderException.class, () -> new Order("a".repeat(101)));
    }
}
