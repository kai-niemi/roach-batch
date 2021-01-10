package io.roach.batch.flatfile;

import java.util.List;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class FlatFileStreamWriter<T> extends AbstractStreamWriter<T> {
    protected LineAggregator<T> lineAggregator;

    public FlatFileStreamWriter() {
        this.setExecutionContextName(ClassUtils.getShortName(FlatFileStreamWriter.class));
    }

    public void setLineAggregator(LineAggregator<T> lineAggregator) {
        this.lineAggregator = lineAggregator;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(lineAggregator, "A LineAggregator must be provided.");
    }

    @Override
    public String doWrite(List<? extends T> items) {
        StringBuilder lines = new StringBuilder();
        for (T item : items) {
            lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
        }
        return lines.toString();
    }
}
