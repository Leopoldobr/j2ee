package br.com.leopoldobr.j2ee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Composite Entity: raiz que controla o ciclo de vida do pedido e de seus itens. */
@Entity(name = "PurchaseOrder")
@Table(name = "purchase_orders")
public class Order {
    @Id @Column(length = 36) private String id;
    @Version private long version;
    @Column(nullable = false, length = 100) private String customer;
    @Column(nullable = false) private Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status;
    @Column(nullable = false) private int processingCount;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id") private List<OrderLine> lines = new ArrayList<>();

    protected Order() { }

    public Order(String customer) {
        if (customer == null || customer.isBlank() || customer.strip().length() > 100) {
            throw new InvalidOrderException("Informe um cliente com até 100 caracteres.");
        }
        this.id = UUID.randomUUID().toString();
        this.customer = customer.strip();
        this.createdAt = Instant.now();
        this.status = OrderStatus.PENDING;
    }

    public void addLine(Product product, int quantity) {
        requirePending();
        if (product == null) throw new InvalidOrderException("Produto obrigatório.");
        if (lines.size() >= 20) throw new InvalidOrderException("Limite de 20 produtos por pedido.");
        if (lines.stream().anyMatch(line -> line.getProductId().equals(product.getId()))) {
            throw new InvalidOrderException("Produto repetido no pedido.");
        }
        lines.add(new OrderLine(this, product, quantity));
    }

    public void changeQuantity(long productId, int quantity) {
        requirePending();
        findLine(productId).changeQuantity(quantity);
    }

    public void removeLine(long productId) {
        requirePending();
        OrderLine line = findLine(productId);
        if (lines.size() == 1) throw new InvalidOrderException("O pedido precisa de pelo menos um item.");
        lines.remove(line); // orphanRemoval remove a linha persistida no flush.
    }

    public void validateForSubmission() {
        if (lines.isEmpty()) throw new InvalidOrderException("Selecione pelo menos um produto.");
    }

    /** Uma segunda entrega da mesma mensagem não reaplica o efeito de negócio. */
    public boolean process() {
        if (status == OrderStatus.PROCESSED) return false;
        validateForSubmission();
        status = OrderStatus.PROCESSED;
        processingCount++;
        return true;
    }

    private void requirePending() {
        if (status != OrderStatus.PENDING) throw new InvalidOrderException("Pedido já processado.");
    }

    private OrderLine findLine(long productId) {
        return lines.stream().filter(line -> line.getProductId() == productId).findFirst()
                .orElseThrow(() -> new InvalidOrderException("Item não encontrado."));
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public int getProcessingCount() { return processingCount; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public BigDecimal getTotal() {
        return lines.stream().map(OrderLine::getTotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
