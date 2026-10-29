package br.com.leopoldobr.j2ee.dto;

import br.com.leopoldobr.j2ee.domain.InvalidOrderException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateOrderCommandTest {
    @Test void defensivelyCopiesInput() {
        var source = new ArrayList<>(List.of(new CreateOrderCommand.Line(1, 2)));
        var command = new CreateOrderCommand(" Leo ", source);
        source.clear();
        assertEquals(1, command.lines().size());
        assertEquals("Leo", command.customer());
        assertThrows(UnsupportedOperationException.class, () -> command.lines().clear());
    }
    @Test void rejectsDuplicatesAndNullElements() {
        assertThrows(InvalidOrderException.class, () -> new CreateOrderCommand("Leo", List.of(
                new CreateOrderCommand.Line(1, 1), new CreateOrderCommand.Line(1, 2))));
        assertThrows(InvalidOrderException.class, () -> new CreateOrderCommand("Leo", Arrays.asList((CreateOrderCommand.Line) null)));
        assertThrows(InvalidOrderException.class, () -> new CreateOrderCommand("Leo", List.of()));
    }
}
