package com.socialmedia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Database connection settings live in application.yml / env vars
 * (SPRING_DATASOURCE_URL, USERNAME, PASSWORD). This config exposes uploaded files.
 */
@Configuration
public class DatabaseConfig implements WebMvcConfigurer {

  @Value("${app.upload-dir}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = "file:" + uploadDir.replace("\\", "/");
    if (!location.endsWith("/")) {
      location += "/";
    }
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
