package io.roach.batch.flatfile;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import io.roach.batch.config.DataSourceFactory;
import io.roach.batch.flatfile.schema.FlatFileSchema;

public class JdbcBatchWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private DataSourceProperties properties;

        private DataSourceFactory dataSourceFactory;

        private FlatFileSchema flatFileSchema;

        public Builder setProperties(DataSourceProperties properties) {
            this.properties = properties;
            return this;
        }

        public Builder setDataSourceFactory(DataSourceFactory dataSourceFactory) {
            this.dataSourceFactory = dataSourceFactory;
            return this;
        }

        public Builder setFlatFileSchema(FlatFileSchema flatFileSchema) {
            this.flatFileSchema = flatFileSchema;
            return this;
        }

        public ItemWriter<List<String>> build() {
            DataSource dataSource = dataSourceFactory.createDataSource(properties);

            if (StringUtils.hasLength(flatFileSchema.getTableSchema().getCreate())) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.execute(flatFileSchema.getTableSchema().getCreate());
            }

            JdbcBatchItemWriter<List<String>> itemWriter = new JdbcBatchItemWriter<>();
            itemWriter.setSql(flatFileSchema.getTableSchema().getInsert());
            itemWriter.setDataSource(dataSourceFactory.createDataSource(properties));
            itemWriter.setItemPreparedStatementSetter((values, preparedStatement) -> {
                int i = 1;
                for (String v : values) {
                    preparedStatement.setString(i++, v);
                }
            });
            itemWriter.afterPropertiesSet();
            return itemWriter;
        }
    }
}
