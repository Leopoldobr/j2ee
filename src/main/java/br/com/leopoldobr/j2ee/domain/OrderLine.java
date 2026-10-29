package br.com.leopoldobr.j2ee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Objeto dependente: somente Order cria, altera e remove seus itens. */
@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(nullable = false) private Long productId;
    @Column(nullable = false, length = 100) private String nameAtPurchase;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity;

    protected OrderLine() { }

    OrderLine(Order order, Product product, int quantity) {
        this.order = order;
        this.productId = product.getId();
        this.nameAtPurchase = product.getName();
        this.unitPrice = product.getPrice();
        changeQuantity(quantity);
    }

    void changeQuantity(int quantity) {
        if (quantity < 1 || quantity > 100) {
            throw new InvalidOrderException("A quantidade deve estar entre 1 e 100.");
        }
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public String getNameAtPurchase() { return nameAtPurchase; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
