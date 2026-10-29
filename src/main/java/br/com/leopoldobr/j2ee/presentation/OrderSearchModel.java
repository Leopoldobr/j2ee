package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.business.ValueListHandler;
import br.com.leopoldobr.j2ee.dto.OrderSummaryDto;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import java.io.Serializable;

@SessionScoped
public class OrderSearchModel implements Serializable {
    @Inject private OrderDelegate delegate;
    private ValueListHandler<OrderSummaryDto> handler;

    public synchronized OrderListPage page(boolean refresh, int page) {
        if (handler == null || refresh) {
            handler = new ValueListHandler<>(delegate.recent(ValueListHandler.MAX_RESULTS + 1), 5);
        }
        handler.moveTo(page);
        return new OrderListPage(handler.current(), handler.getPageIndex(), handler.getPageCount(),
                handler.getSize(), handler.isTruncated());
    }
    public synchronized void invalidate() { handler = null; }
}
