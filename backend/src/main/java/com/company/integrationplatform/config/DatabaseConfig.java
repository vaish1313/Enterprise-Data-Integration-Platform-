package com.company.integrationplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.company.integrationplatform")
public class DatabaseConfig {
    // DataSource and JPA are auto-configured via application.yml
    // This class enables transaction management and JPA repository scanning
}
