package br.com.leopoldobr.j2ee.business;

import br.com.leopoldobr.j2ee.domain.*;
import br.com.leopoldobr.j2ee.integration.ProductDao;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderAssemblerTest {
    @Test void combinesCurrentCatalogWithHistoricalValuesInOneBatch() throws Exception {
        var products = mock(ProductDao.class);
        when(products.findByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L,
                new Product(1L, "Nome alterado", new BigDecimal("999.00"))));
        var assembler = new OrderAssembler();
        var field = OrderAssembler.class.getDeclaredField("products");
        field.setAccessible(true); field.set(assembler, products);
        var order = new Order("Leo");
        order.addLine(new Product(1L, "Nome original", new BigDecimal("10.00")), 2);
        order.addLine(new Product(2L, "Removido", new BigDecimal("5.00")), 1);
        var dto = assembler.details(order);
        assertEquals("Nome original", dto.getLines().get(0).getNameAtPurchase());
        assertEquals("Nome alterado", dto.getLines().get(0).getCurrentName());
        assertEquals(new BigDecimal("25.00"), dto.getTotal());
        assertEquals("Produto fora do catálogo", dto.getLines().get(1).getCurrentName());
        verify(products, times(1)).findByIds(Set.of(1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> dto.getLines().clear());
    }
}
