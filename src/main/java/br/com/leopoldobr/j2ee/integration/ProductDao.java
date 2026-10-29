package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.domain.Product;
import jakarta.ejb.Local;
import java.util.*;

@Local
public interface ProductDao {
    Optional<Product> find(long id);
    List<Product> findAll();
    Map<Long, Product> findByIds(Set<Long> ids);
}
