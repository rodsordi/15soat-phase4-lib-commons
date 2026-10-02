package br.com.fiap.phase4.commons.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

public class LogInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String W3C_TRACEPARENT_HEADER = "traceparent";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = resolveTraceId(request);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);

        response.setHeader(TRACE_ID_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(W3C_TRACEPARENT_HEADER);
        if (traceparent != null && traceparent.startsWith("00-")) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2 && parts[1].length() == 32) {
                return parts[1];
            }
        }

        String xTraceId = request.getHeader(TRACE_ID_HEADER);
        if (xTraceId != null && !xTraceId.isBlank()) {
            return xTraceId.trim();
        }

        return UUID.randomUUID().toString().replace("-", "");
    }
}
