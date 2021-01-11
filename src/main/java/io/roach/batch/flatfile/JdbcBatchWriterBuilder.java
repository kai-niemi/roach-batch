package io.roach.batch.flatfile;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import io.roach.batch.flatfile.schema.FlatFileSchema;

public class JdbcBatchWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private DataSource dataSource;

        private FlatFileSchema flatFileSchema;

        public Builder setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder setFlatFileSchema(FlatFileSchema flatFileSchema) {
            this.flatFileSchema = flatFileSchema;
            return this;
        }

        public ItemWriter<List<String>> build() {
            if (StringUtils.hasLength(flatFileSchema.getTableSchema().getCreate())) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.execute(flatFileSchema.getTableSchema().getCreate());
            }

            JdbcBatchItemWriter<List<String>> itemWriter = new JdbcBatchItemWriter<>();
            itemWriter.setSql(flatFileSchema.getTableSchema().getInsert());
            itemWriter.setDataSource(dataSource);
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
