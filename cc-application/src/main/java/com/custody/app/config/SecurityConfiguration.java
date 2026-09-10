package com.custody.app.config;

import com.custody.app.audit.ApiAuditPublisher;
import com.custody.app.constants.ApiRoutes;
import com.custody.app.exception.handler.ErrorResponseFactory;
import com.custody.identity.error.IdentityErrorCatalog;
import com.custody.identity.service.IdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetails(IdentityService identity) {
    return name ->
        identity
            .credential(name)
            .map(
                authenticationCredential ->
                    User.withUsername(authenticationCredential.username())
                        .password(authenticationCredential.hash())
                        .disabled(!authenticationCredential.active())
                        .authorities("USER")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Account unavailable"));
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http, ErrorResponseFactory errors, ApiAuditPublisher auditPublisher)
      throws Exception {
    http.authorizeHttpRequests(
        authorizationRules ->
            authorizationRules
                .requestMatchers(
                    "/", "/index.html", "/app.js", "/style.css", "/favicon.ico", ApiRoutes.CSRF)
                .permitAll()
                .anyRequest()
                .authenticated());
    http.formLogin(
        formLogin ->
            formLogin
                .loginProcessingUrl(ApiRoutes.LOGIN)
                .successHandler(
                    (httpServletRequest, httpServletResponse, authentication) -> {
                      httpServletResponse.setContentType("application/json");
                      httpServletResponse.getWriter().write("{\"authenticated\":true}");
                      auditPublisher.record(
                          httpServletRequest,
                          200,
                          authentication.getName(),
                          authentication.getName(),
                          "AUTHENTICATION_SUCCESS",
                          0);
                    })
                .failureHandler(
                    (httpServletRequest, httpServletResponse, authenticationException) -> {
                      errors.write(
                          httpServletResponse, IdentityErrorCatalog.AUTHENTICATION_REQUIRED);
                      auditPublisher.record(
                          httpServletRequest,
                          httpServletResponse.getStatus(),
                          "ANONYMOUS",
                          httpServletRequest.getParameter("username"),
                          "AUTHENTICATION_FAILURE",
                          0);
                    }));
    http.logout(
        logout ->
            logout
                .logoutUrl(ApiRoutes.LOGOUT)
                .logoutSuccessHandler(
                    (httpServletRequest, httpServletResponse, authentication) -> {
                      httpServletResponse.setStatus(204);
                      String actor =
                          authentication == null ? "ANONYMOUS" : authentication.getName();
                      auditPublisher.record(httpServletRequest, 204, actor, actor, "LOGOUT", 0);
                    }));
    http.exceptionHandling(
        exceptionHandling ->
            exceptionHandling
                .authenticationEntryPoint(
                    (httpServletRequest, httpServletResponse, authenticationException) -> {
                      errors.write(
                          httpServletResponse, IdentityErrorCatalog.AUTHENTICATION_REQUIRED);
                      auditPublisher.record(
                          httpServletRequest,
                          httpServletResponse.getStatus(),
                          "ANONYMOUS",
                          "",
                          "AUTHENTICATION_REQUIRED",
                          0);
                    })
                .accessDeniedHandler(
                    (httpServletRequest, httpServletResponse, accessDeniedException) -> {
                      errors.write(httpServletResponse, IdentityErrorCatalog.ACTION_FORBIDDEN);
                      String actor =
                          httpServletRequest.getUserPrincipal() == null
                              ? "ANONYMOUS"
                              : httpServletRequest.getUserPrincipal().getName();
                      auditPublisher.record(
                          httpServletRequest,
                          httpServletResponse.getStatus(),
                          actor,
                          "",
                          "ACCESS_DENIED",
                          0);
                    }));
    http.headers(
        securityHeaders ->
            securityHeaders.contentSecurityPolicy(
                contentSecurityPolicy ->
                    contentSecurityPolicy.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
    return http.build();
  }
}
