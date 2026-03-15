package com.juno.healthapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.beans.factory.annotation.Qualifier;
import javax.sql.DataSource;

@Configuration
public class ClickHouseDataSourceConfig {

    @Bean(name = "clickHouseDataSource")
    @ConfigurationProperties("spring.datasource.clickhouse")
    public DataSource clickHouseDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate(
            @Qualifier("clickHouseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}