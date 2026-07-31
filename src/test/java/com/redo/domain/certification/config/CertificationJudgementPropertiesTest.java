package com.redo.domain.certification.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CertificationJudgementPropertiesTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void rejectsMaxPoolSizeSmallerThanCorePoolSize() {
        CertificationJudgementProperties properties =
                new CertificationJudgementProperties(
                        Duration.ofSeconds(210),
                        4,
                        2,
                        20,
                        4
                );

        assertThat(validator.validate(properties))
                .extracting(ConstraintViolation::getMessage)
                .contains("maxPoolSize must be greater than or equal to corePoolSize");
    }

    @Test
    void rejectsSingleThreadedTimeoutScheduler() {
        CertificationJudgementProperties properties =
                new CertificationJudgementProperties(
                        Duration.ofSeconds(210),
                        2,
                        4,
                        20,
                        1
                );

        assertThat(validator.validate(properties))
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("timeoutSchedulerPoolSize");
    }
}
