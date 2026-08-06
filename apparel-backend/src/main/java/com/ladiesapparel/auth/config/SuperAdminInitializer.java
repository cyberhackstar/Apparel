package com.ladiesapparel.auth.config;

import com.ladiesapparel.auth.Role;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.auth.UserRepository;
import com.ladiesapparel.auth.config.SuperAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SuperAdminProperties superAdminProperties;

  @Override
  @Transactional
  public void run(String... args) {
    if (userRepository.countByRole(Role.SUPER_ADMIN) == 0) {
      if (userRepository.existsByEmail(superAdminProperties.getEmail())) {
        log.warn("Cannot create Super Admin: User with email {} already exists.", superAdminProperties.getEmail());
        return;
      }

      User superAdmin = User.builder()
          .fullName(superAdminProperties.getFullName())
          .email(superAdminProperties.getEmail())
          .password(passwordEncoder.encode(superAdminProperties.getPassword()))
          .phone(superAdminProperties.getPhone())
          .role(Role.SUPER_ADMIN)
          .enabled(true)
          .blocked(false)
          .build();

      userRepository.save(superAdmin);
      log.info("Default Super Admin account created successfully with email: {}", superAdminProperties.getEmail());
    } else {
      log.info("Super Admin account already exists. Skipping initialization.");
    }
  }
}