package com.carloslonghi.bcb.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket por IP em POST /auth e POST /clients. Estourou: 429 com Retry-After, pelo /error do Spring Boot
 * (mesmo JSON dos outros erros). Os buckets ficam num cache Caffeine e somem depois de um periodo sem uso.
 * Em memoria: basta com uma instancia do backend. Atras de proxy reverso, ligue server.forward-headers-strategy,
 * senao todos os clientes chegam com o IP do proxy e dividem o mesmo bucket.
 * Nao e um bean, para o Spring Boot nao o registrar tambem como filtro do servlet: entra so na cadeia do SecurityConfig.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long MAX_TRACKED_KEYS = 100_000;

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
        // Bucket sem uso pelo periodo mais longo ja estaria cheio de novo: pode sair do cache
        Duration longest = Collections.max(List.of(properties.auth().period(), properties.register().period()));
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(longest)
                .maximumSize(MAX_TRACKED_KEYS)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || ruleName(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruleName = ruleName(request);
        RateLimitProperties.Rule rule = "auth".equals(ruleName) ? properties.auth() : properties.register();
        Bucket bucket = buckets.get(ruleName + ":" + request.getRemoteAddr(), key -> newBucket(rule));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                "Muitas tentativas. Aguarde %d segundo(s) e tente de novo.".formatted(seconds));
    }

    private static String ruleName(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        return switch (request.getRequestURI()) {
            case "/auth" -> "auth";
            case "/clients" -> "register";
            default -> null;
        };
    }

    private static Bucket newBucket(RateLimitProperties.Rule rule) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.capacity())
                        .refillGreedy(rule.capacity(), rule.period())
                        .build())
                .build();
    }
}
