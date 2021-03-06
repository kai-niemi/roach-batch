package io.roach.batch.flatfile;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.roach.batch.flatfile.schema.FlatFileSchema;

public abstract class FlatFileResourceWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private FlatFileSchema flatFileSchema;

        private Resource outputResource;

        private String delimiter = ",";

        public Builder setFlatFileSchema(FlatFileSchema flatFileSchema) {
            this.flatFileSchema = flatFileSchema;
            return this;
        }

        public Builder setOutputResource(Resource outputResource) {
            this.outputResource = outputResource;
            return this;
        }

        public Builder setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public ItemWriter<List<String>> build() {
            DelimitedLineAggregator<List<String>> lineAggregator = new DelimitedLineAggregator<>();
            lineAggregator.setDelimiter(delimiter);

            return new FlatFileItemWriterBuilder<List<String>>()
                    .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                    .resource(outputResource)
                    .append(false)
                    .shouldDeleteIfExists(true)
                    .saveState(false)
                    .transactional(false)
                    .forceSync(false)
                    .lineAggregator(lineAggregator)
                    .headerCallback(writer -> writer
                            .write(StringUtils.collectionToDelimitedString(flatFileSchema.allFieldNames(), delimiter)))
                    .build();
        }
    }
}
