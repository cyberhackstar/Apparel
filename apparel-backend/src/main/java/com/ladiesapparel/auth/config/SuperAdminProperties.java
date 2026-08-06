package com.ladiesapparel.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.super-admin")
@Getter
@Setter
public class SuperAdminProperties {
  private String fullName;
  private String email;
  private String password;
  private String phone;
}