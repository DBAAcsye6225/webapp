package com.csye6225.webapp.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final String TIMER_SAMPLE_ATTRIBUTE = ApiMetricsInterceptor.class.getName() + ".timerSample";
    private static final String ENDPOINT_ATTRIBUTE = ApiMetricsInterceptor.class.getName() + ".endpoint";

    private final MeterRegistry meterRegistry;

    public ApiMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        request.setAttribute(ENDPOINT_ATTRIBUTE, resolveEndpoint(request));
        request.setAttribute(TIMER_SAMPLE_ATTRIBUTE, Timer.start(meterRegistry));
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        String endpoint = (String) request.getAttribute(ENDPOINT_ATTRIBUTE);
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = resolveEndpoint(request);
        }

        meterRegistry.counter("api.calls.count", "endpoint", endpoint).increment();

        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_SAMPLE_ATTRIBUTE);
        if (sample != null) {
            sample.stop(meterRegistry.timer("api.calls.time", "endpoint", endpoint));
        }
    }

    private String resolveEndpoint(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = pattern instanceof String matchedPattern && !matchedPattern.isBlank()
                ? matchedPattern
                : request.getRequestURI();
        return request.getMethod() + "_" + path;
    }
}