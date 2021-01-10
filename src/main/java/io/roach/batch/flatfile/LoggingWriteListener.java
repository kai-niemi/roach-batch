package io.roach.batch.flatfile;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;

public class LoggingWriteListener<T> implements ItemWriteListener<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private long linesWritten;

    public LoggingWriteListener() {
    }

    @Override
    public void beforeWrite(List<? extends T> items) {
    }

    @Override
    public void afterWrite(List<? extends T> items) {
        linesWritten += items.size();
    }

    @Override
    public void onWriteError(Exception ex, List<? extends T> items) {
        logger.error("Write error near line {}: {}: {}", linesWritten, items, ex);
    }
}
