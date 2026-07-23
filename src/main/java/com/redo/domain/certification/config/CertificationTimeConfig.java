package com.redo.domain.certification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class CertificationTimeConfig {

    public static final String CERTIFICATION_CLOCK = "certificationClock";
    public static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Bean(CERTIFICATION_CLOCK)
    public Clock certificationClock() {
        return Clock.system(SEOUL_ZONE);
    }
}
