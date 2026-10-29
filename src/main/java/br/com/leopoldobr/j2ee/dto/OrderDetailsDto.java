package br.com.leopoldobr.j2ee.dto;

import br.com.leopoldobr.j2ee.domain.OrderStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDetailsDto implements Serializable {
    private final String id, customer;
    private final OrderStatus status;
    private final Instant createdAt;
    private final List<OrderLineDto> lines;
    private final BigDecimal total;
    private final int processingCount;
    public OrderDetailsDto(String id, String customer, OrderStatus status, Instant createdAt,
                           List<OrderLineDto> lines, BigDecimal total, int processingCount) {
        this.id = id; this.customer = customer; this.status = status; this.createdAt = createdAt;
        this.lines = List.copyOf(lines); this.total = total; this.processingCount = processingCount;
    }
    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public String getStatus() { return status.name(); }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLineDto> getLines() { return lines; }
    public BigDecimal getTotal() { return total; }
    public int getProcessingCount() { return processingCount; }
}
