package com.custody.app.config;

import com.custody.app.audit.ApiAuditInterceptor;
import com.custody.app.constants.ApiRoutes;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {
  private final ApiAuditInterceptor auditInterceptor;

  WebConfiguration(ApiAuditInterceptor apiAuditInterceptor) {
    this.auditInterceptor = apiAuditInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(auditInterceptor).addPathPatterns(ApiRoutes.BASE + "/**");
  }
}
