package br.com.leopoldobr.j2ee.dto;

import br.com.leopoldobr.j2ee.domain.OrderStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public final class OrderSummaryDto implements Serializable {
    private final String id, customer;
    private final OrderStatus status;
    private final BigDecimal total;
    private final Instant createdAt;
    public OrderSummaryDto(String id, String customer, OrderStatus status, BigDecimal total, Instant createdAt) {
        this.id = id; this.customer = customer; this.status = status; this.total = total; this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public String getStatus() { return status.name(); }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
}
