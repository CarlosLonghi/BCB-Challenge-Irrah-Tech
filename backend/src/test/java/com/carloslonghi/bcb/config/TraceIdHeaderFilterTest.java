package com.carloslonghi.bcb.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceIdHeaderFilterTest {

    private final Tracer tracer = mock(Tracer.class);
    private final TraceIdHeaderFilter filter = new TraceIdHeaderFilter(tracer);

    @Test
    @DisplayName("devolve o traceId do span atual no header X-Trace-Id e segue a cadeia")
    void addsTraceId() throws Exception {
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getHeader(TraceIdHeaderFilter.HEADER)).isEqualTo("abc123");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("sem span (tracing desligado): não põe o header, mas segue a cadeia")
    void withoutSpan() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getHeader(TraceIdHeaderFilter.HEADER)).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
