package io.roach.batch.flatfile;

import java.io.Writer;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.util.StringUtils;

import io.roach.batch.flatfile.schema.FlatFileSchema;

public class FlatFileStreamWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private FlatFileSchema flatFileSchema;

        private Writer outputWriter;

        private String delimiter = ",";

        public Builder setFlatFileSchema(FlatFileSchema flatFileSchema) {
            this.flatFileSchema = flatFileSchema;
            return this;
        }

        public Builder setOutputWriter(Writer outputWriter) {
            this.outputWriter = outputWriter;
            return this;
        }

        public Builder setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public ItemWriter<List<String>> build() {
            DelimitedLineAggregator<List<String>> lineAggregator = new DelimitedLineAggregator<>();
            lineAggregator.setDelimiter(delimiter);

            FlatFileStreamWriter<List<String>> itemWriter = new FlatFileStreamWriter<>();
            itemWriter.setLineAggregator(lineAggregator);
            itemWriter.setOutputBufferedWriter(outputWriter);
            itemWriter.setHeaderCallback(writer -> writer
                    .write(StringUtils.collectionToDelimitedString(flatFileSchema.allFieldNames(), delimiter))
            );

            return itemWriter;
        }
    }
}
