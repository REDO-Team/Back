package com.redo.domain.certification.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.certification.judgement")
public record CertificationJudgementProperties(
        @NotNull Duration timeout,
        @Min(1) int corePoolSize,
        @Min(1) int maxPoolSize,
        @Min(0) int queueCapacity,
        @Min(2) int timeoutSchedulerPoolSize
) {

    @AssertTrue(message = "maxPoolSize must be greater than or equal to corePoolSize")
    public boolean isPoolSizeConsistent() {
        return maxPoolSize >= corePoolSize;
    }
}
