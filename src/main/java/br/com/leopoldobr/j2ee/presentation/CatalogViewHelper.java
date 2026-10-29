package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.dto.ProductDto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import java.util.List;

/** View Helper usado pelo Dispatcher View: carrega ao ser consultado pela JSP. */
@Named("catalogView") @RequestScoped
public class CatalogViewHelper {
    @Inject private OrderDelegate delegate;
    private List<ProductDto> products;
    public List<ProductDto> getProducts() {
        if (products == null) products = delegate.products();
        return products; // EL pode invocar o getter mais de uma vez na mesma renderização.
    }
}
