package com.carloslonghi.bcb.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Devolve o traceId da requisicao no header X-Trace-Id, o mesmo que aparece nos logs (trace.id), inclusive
 * nos do worker que processar a mensagem. Com ele, um erro visto no navegador leva direto aos logs.
 * Roda logo depois do filtro de observation do Spring, que abre o span, e antes do Spring Security,
 * para o header sair tambem nos 401.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class TraceIdHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Span span = tracer.currentSpan();
        if (span != null) {
            response.setHeader(HEADER, span.context().traceId());
        }
        chain.doFilter(request, response);
    }
}
