package br.com.leopoldobr.j2ee.presentation;

import br.com.leopoldobr.j2ee.domain.InvalidOrderException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrontControllerTest {
    private HttpServletRequest form(String[] ids, String[] quantities) {
        var request = mock(HttpServletRequest.class);
        when(request.getParameterValues("productId")).thenReturn(ids);
        when(request.getParameterValues("quantity")).thenReturn(quantities);
        when(request.getParameter("customer")).thenReturn("Leo");
        return request;
    }
    @Test void ignoresUnselectedProductsAndClientPrices() {
        var request = form(new String[]{"1", "2"}, new String[]{"0", "3"});
        var command = FrontController.parseCommand(request);
        assertEquals(1, command.lines().size());
        assertEquals(2, command.lines().get(0).productId());
        verify(request, never()).getParameter("price");
    }
    @Test void rejectsMismatchedNegativeAndMalformedInputs() {
        assertThrows(InvalidOrderException.class, () -> FrontController.parseCommand(form(new String[]{"1"}, new String[]{})));
        assertThrows(InvalidOrderException.class, () -> FrontController.parseCommand(form(new String[]{"1"}, new String[]{"-1"})));
        assertThrows(NumberFormatException.class, () -> FrontController.parseCommand(form(new String[]{"1 OR 1=1"}, new String[]{"1"})));
    }
    @Test void rejectsMissingOrIncorrectCsrfToken() {
        assertFalse(RequestFilter.validToken("secret", null));
        assertFalse(RequestFilter.validToken("secret", "wrong"));
        assertTrue(RequestFilter.validToken("secret", "secret"));
    }
}
