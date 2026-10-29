package br.com.leopoldobr.j2ee.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Value Object do catálogo do livro = DTO; não é uma entidade JPA. */
public final class ProductDto implements Serializable {
    private final long id;
    private final String name;
    private final BigDecimal price;
    public ProductDto(long id, String name, BigDecimal price) {
        this.id = id; this.name = name; this.price = price;
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
