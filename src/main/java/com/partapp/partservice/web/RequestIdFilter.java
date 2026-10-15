package com.partapp.partservice.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String id = req.getHeader("X-Request-Id");
    if (id == null || id.isBlank())
      id = "req_" + UUID.randomUUID();
    req.setAttribute("requestId", id);
    res.setHeader("X-Request-Id", id);
    long start = System.nanoTime();
    try {
      chain.doFilter(req, res);
    } finally {
      log.info("request_id={} method={} path={} status={} duration_ms={}", id, req.getMethod(), req.getRequestURI(),
          res.getStatus(), (System.nanoTime() - start) / 1_000_000);
    }
  }
}
