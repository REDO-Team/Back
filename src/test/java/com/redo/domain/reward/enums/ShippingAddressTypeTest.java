package com.redo.domain.reward.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingAddressTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndDeserializesEnumCode() throws Exception {
        assertThat(objectMapper.readValue("\"HOME\"", ShippingAddressType.class))
                .isEqualTo(ShippingAddressType.HOME);
        assertThat(objectMapper.writeValueAsString(ShippingAddressType.HOME))
                .isEqualTo("\"HOME\"");
    }
}
