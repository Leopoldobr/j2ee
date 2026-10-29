package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.dto.OrderSummaryDto;
import java.util.List;

/** Modelo completo preparado antes do forward (Service to Worker). */
public final class OrderListPage {
    private final List<OrderSummaryDto> items;
    private final int index, pageCount, size;
    private final boolean truncated;
    public OrderListPage(List<OrderSummaryDto> items, int index, int pageCount, int size, boolean truncated) {
        this.items = List.copyOf(items); this.index = index; this.pageCount = pageCount;
        this.size = size; this.truncated = truncated;
    }
    public List<OrderSummaryDto> getItems() { return items; }
    public int getIndex() { return index; }
    public int getPageCount() { return pageCount; }
    public int getSize() { return size; }
    public boolean isTruncated() { return truncated; }
}
