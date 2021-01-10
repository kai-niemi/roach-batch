package io.roach.batch.flatfile;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractStreamWriter<T> extends AbstractItemStreamItemWriter<T> implements InitializingBean {
    public static final String DEFAULT_LINE_SEPARATOR = System.getProperty("line.separator");

    protected static final Logger logger = LoggerFactory.getLogger(AbstractFileItemWriter.class);

    protected String lineSeparator = DEFAULT_LINE_SEPARATOR;

    private Writer outputBufferedWriter;

    private FlatFileHeaderCallback headerCallback;

    private FlatFileFooterCallback footerCallback;

    public AbstractStreamWriter<T> setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public void setOutputBufferedWriter(Writer outputBufferedWriter) {
        this.outputBufferedWriter = outputBufferedWriter;
    }

    public void setHeaderCallback(FlatFileHeaderCallback headerCallback) {
        this.headerCallback = headerCallback;
    }

    public void setFooterCallback(FlatFileFooterCallback footerCallback) {
        this.footerCallback = footerCallback;
    }

    /**
     * Writes out a string followed by a "new line", where the format of the new
     * line separator is determined by the underlying operating system.
     *
     * @param items list of items to be written to output stream
     * @throws WriteFailedException if an error occurs while writing items to the output stream
     */
    @Override
    public void write(List<? extends T> items) throws WriteFailedException {
        if (logger.isDebugEnabled()) {
            logger.debug("Writing to file with " + items.size() + " items.");
        }

        String lines = doWrite(items);
        try {
            outputBufferedWriter.write(lines);
            outputBufferedWriter.flush();
        } catch (IOException e) {
            throw new WriteFailedException("Could not write data. The stream may be corrupt.", e);
        }
    }

    /**
     * Write out a string of items followed by a "new line", where the format of the new
     * line separator is determined by the underlying operating system.
     *
     * @param items to be written
     * @return written lines
     */
    protected abstract String doWrite(List<? extends T> items);

    /**
     * @see ItemStream#close()
     */
    @Override
    public void close() {
        super.close();
        try {
            if (footerCallback != null) {
                footerCallback.writeFooter(outputBufferedWriter);
                outputBufferedWriter.flush();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Failed to write footer before closing", e);
        }
    }

    /**
     * @see ItemStream#open(ExecutionContext)
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);

        if (headerCallback != null) {
            try {
                headerCallback.writeHeader(outputBufferedWriter);
                outputBufferedWriter.write(lineSeparator);
            } catch (IOException e) {
                throw new ItemStreamException("Could not write headers.  The file may be corrupt.", e);
            }
        }
    }

    /**
     * @see ItemStream#update(ExecutionContext)
     */
    @Override
    public void update(ExecutionContext executionContext) {
        super.update(executionContext);
        Assert.notNull(executionContext, "ExecutionContext must not be null");
    }
}
