package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.domain.Product;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Singleton @Startup
public class CatalogSeeder {
    @PersistenceContext private EntityManager em;
    @PostConstruct
    public void seed() {
        if (em.find(Product.class, 1L) != null) return;
        em.persist(new Product(1L, "Teclado", new BigDecimal("180.00")));
        em.persist(new Product(2L, "Mouse", new BigDecimal("75.50")));
        em.persist(new Product(3L, "Monitor", new BigDecimal("950.00")));
    }
}
