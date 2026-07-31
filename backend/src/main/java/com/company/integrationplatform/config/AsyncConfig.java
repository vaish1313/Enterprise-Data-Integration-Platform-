package com.company.integrationplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Async executor for long-running ingestion and synchronization jobs.
     *
     * <p>Wrapped in {@link DelegatingSecurityContextAsyncTaskExecutor} so that
     * the caller's {@link org.springframework.security.core.context.SecurityContext}
     * is captured on the request thread (where {@code JwtAuthenticationFilter}
     * sets it) and propagated into every async worker thread before execution.
     *
     * <p>This is the required pattern for using {@code @PreAuthorize} or
     * {@code SecurityContextHolder} inside {@code @Async} methods because
     * {@code ThreadLocal} storage is <em>not</em> shared across thread-pool
     * threads; {@code MODE_INHERITABLETHREADLOCAL} would not help here because
     * pool threads are created ahead of time, not as children of the
     * request thread.
     */
    @Bean(name = "jobExecutor")
    public Executor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("jobExecutor-");
        executor.initialize();
        // Wrap with DelegatingSecurityContextAsyncTaskExecutor so the
        // SecurityContext from the request thread is propagated to async threads.
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
