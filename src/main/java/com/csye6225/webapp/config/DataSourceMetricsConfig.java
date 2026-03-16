package com.csye6225.webapp.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Configuration
public class DataSourceMetricsConfig {

    @Bean
    public BeanPostProcessor dataSourceMetricsBeanPostProcessor(MeterRegistry meterRegistry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }

                if (!"dataSource".equals(beanName) || bean instanceof ProxyDataSource) {
                    return bean;
                }

                QueryExecutionListener listener = new QueryExecutionListener() {
                    @Override
                    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
                    }

                    @Override
                    public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
                        if (queryInfoList == null || queryInfoList.isEmpty()) {
                            recordQueryTiming(meterRegistry, "UNKNOWN", executionInfo.getElapsedTime());
                            return;
                        }

                        for (QueryInfo queryInfo : queryInfoList) {
                            recordQueryTiming(meterRegistry, determineQueryType(queryInfo.getQuery()), executionInfo.getElapsedTime());
                        }
                    }
                };

                return ProxyDataSourceBuilder.create(dataSource)
                        .name("webapp-data-source")
                        .listener(listener)
                        .build();
            }
        };
    }

    private static void recordQueryTiming(MeterRegistry meterRegistry, String queryType, long elapsedTimeMs) {
        Timer.builder("db.query.time")
                .tag("query", queryType)
                .register(meterRegistry)
                .record(elapsedTimeMs, TimeUnit.MILLISECONDS);
    }

    private static String determineQueryType(String query) {
        if (query == null || query.isBlank()) {
            return "UNKNOWN";
        }

        String trimmedQuery = query.trim();
        int separatorIndex = trimmedQuery.indexOf(' ');
        String queryType = separatorIndex > 0 ? trimmedQuery.substring(0, separatorIndex) : trimmedQuery;
        return queryType.toUpperCase(Locale.ROOT);
    }
}