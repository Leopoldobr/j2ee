package br.com.leopoldobr.j2ee.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public final class OrderLineDto implements Serializable {
    private final long productId;
    private final String nameAtPurchase, currentName;
    private final int quantity;
    private final BigDecimal unitPrice, total;
    public OrderLineDto(long productId, String nameAtPurchase, String currentName, int quantity,
                        BigDecimal unitPrice, BigDecimal total) {
        this.productId = productId; this.nameAtPurchase = nameAtPurchase; this.currentName = currentName;
        this.quantity = quantity; this.unitPrice = unitPrice; this.total = total;
    }
    public long getProductId() { return productId; }
    public String getNameAtPurchase() { return nameAtPurchase; }
    public String getCurrentName() { return currentName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotal() { return total; }
}
