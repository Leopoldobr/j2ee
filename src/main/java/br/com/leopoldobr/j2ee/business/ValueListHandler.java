package br.com.leopoldobr.j2ee.business;

import java.io.Serializable;
import java.util.List;

/** Variante com snapshot e cursor, fiel ao Value List Handler do livro. */
public final class ValueListHandler<T extends Serializable> implements Serializable {
    public static final int MAX_RESULTS = 500;
    private final List<T> snapshot;
    private final int pageSize;
    private final boolean truncated;
    private int pageIndex;

    public ValueListHandler(List<T> results, int pageSize) {
        if (pageSize < 1 || pageSize > 50) throw new IllegalArgumentException("Página entre 1 e 50 registros.");
        this.truncated = results.size() > MAX_RESULTS;
        this.snapshot = List.copyOf(results.subList(0, Math.min(results.size(), MAX_RESULTS)));
        this.pageSize = pageSize;
    }

    public synchronized List<T> current() {
        int start = pageIndex * pageSize;
        return List.copyOf(snapshot.subList(start, Math.min(start + pageSize, snapshot.size())));
    }

    public synchronized List<T> next() { if (hasNext()) pageIndex++; return current(); }
    public synchronized List<T> previous() { if (pageIndex > 0) pageIndex--; return current(); }
    public synchronized void reset() { pageIndex = 0; }
    public synchronized void moveTo(int index) {
        if (index < 0 || index >= getPageCount()) throw new IllegalArgumentException("Página inexistente.");
        pageIndex = index;
    }
    public synchronized int getPageIndex() { return pageIndex; }
    public int getPageCount() { return Math.max(1, (snapshot.size() + pageSize - 1) / pageSize); }
    public int getSize() { return snapshot.size(); }
    public boolean isTruncated() { return truncated; }
    public synchronized boolean hasNext() { return pageIndex + 1 < getPageCount(); }
}
