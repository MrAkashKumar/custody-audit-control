package com.custody.app.filter;

import com.custody.app.constants.ApiRoutes;
import com.custody.core.utils.Ids;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

  protected void doFilterInternal(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      FilterChain chain)
      throws ServletException, IOException {
    String id = Ids.next();
    httpServletResponse.setHeader(ApiRoutes.TRACE_HEADER, id);
    long start = System.nanoTime();
    try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", id)) {
      try {
        chain.doFilter(httpServletRequest, httpServletResponse);
      } finally {
        if (httpServletRequest.getRequestURI().startsWith(ApiRoutes.BASE))
          log.info(
              "event=http_request method={} status={} durationMs={}",
              httpServletRequest.getMethod(),
              httpServletResponse.getStatus(),
              (System.nanoTime() - start) / 1_000_000);
      }
    }
  }
}
