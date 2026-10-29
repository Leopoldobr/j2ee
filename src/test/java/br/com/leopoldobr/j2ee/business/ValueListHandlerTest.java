package br.com.leopoldobr.j2ee.business;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class ValueListHandlerTest {
    @Test void navigatesBothWaysAndResets() {
        var handler = new ValueListHandler<>(List.of("a", "b", "c", "d", "e"), 2);
        assertEquals(List.of("a", "b"), handler.current());
        assertEquals(List.of("c", "d"), handler.next());
        assertEquals(List.of("e"), handler.next());
        assertEquals(List.of("e"), handler.next());
        assertEquals(List.of("c", "d"), handler.previous());
        handler.reset();
        assertEquals(List.of("a", "b"), handler.previous());
    }

    @Test void copiesSnapshotAndPages() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        var handler = new ValueListHandler<>(original, 1);
        original.clear();
        assertEquals(2, handler.getSize());
        assertThrows(UnsupportedOperationException.class, () -> handler.current().clear());
        assertThrows(IllegalArgumentException.class, () -> handler.moveTo(2));
        assertThrows(IllegalArgumentException.class, () -> handler.moveTo(-1));
    }

    @Test void capsCacheAndReportsTruncation() {
        var handler = new ValueListHandler<>(IntStream.range(0, 501).boxed().toList(), 50);
        assertTrue(handler.isTruncated());
        assertEquals(500, handler.getSize());
        assertEquals(10, handler.getPageCount());
        handler.moveTo(9);
        assertEquals(499, handler.current().get(49));
        assertFalse(handler.hasNext());
    }

    @Test void emptyResultIsAValidPage() {
        var handler = new ValueListHandler<String>(List.of(), 5);
        assertEquals(List.of(), handler.next());
        assertEquals(1, handler.getPageCount());
        handler.moveTo(0);
        assertFalse(handler.hasNext());
    }

    @Test void rejectsUnboundedPageSize() {
        assertThrows(IllegalArgumentException.class, () -> new ValueListHandler<>(List.of("a"), 0));
        assertThrows(IllegalArgumentException.class, () -> new ValueListHandler<>(List.of("a"), 51));
    }
}
