package io.roach.batch.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Component
public class DataSourceFactory {
    protected final Logger traceLogger = LoggerFactory.getLogger("io.roach.sql_trace");

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maximumPoolSize;

    public DataSource createDataSource(DataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDataSource(properties.initializeDataSourceBuilder().build());
        ds.setAutoCommit(false);
        ds.setMaximumPoolSize(maximumPoolSize);
        ds.setPoolName(properties.getName());
        ds.setConnectionInitSql("select 1");

        return traceLogger.isDebugEnabled() ?
                ProxyDataSourceBuilder
                        .create(ds)
                        .name("SQL-Trace")
                        .asJson()
                        .countQuery()
                        .logQueryBySlf4j(SLF4JLogLevel.DEBUG, "io.roach.sql_trace")
                        .multiline()
                        .build()
                : ds;
    }
}
