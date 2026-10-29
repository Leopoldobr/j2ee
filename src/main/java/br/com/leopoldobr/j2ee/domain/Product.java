package br.com.leopoldobr.j2ee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;

    protected Product() { }

    public Product(Long id, String name, BigDecimal price) {
        if (id == null || id <= 0 || name == null || name.isBlank() || name.length() > 100
                || price == null || price.signum() < 0) {
            throw new InvalidOrderException("Produto inválido.");
        }
        this.id = id;
        this.name = name;
        this.price = price.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
