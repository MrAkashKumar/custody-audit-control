package com.custody.app;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.custody")
@EntityScan("com.custody")
@EnableJpaRepositories("com.custody")
@ConfigurationPropertiesScan("com.custody")
@EnableScheduling
public class CustodyApplication {
  public static void main(String[] args) {
    SpringApplication.run(CustodyApplication.class, args);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
