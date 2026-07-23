package com.redo.global.ai.gemini;

import com.redo.TestcontainersConfiguration;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("gemini-integration")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.ai.gemini.enabled=true")
class GeminiClientIntegrationTest {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @Autowired
    private GeminiClient geminiClient;

    @Test
    void generatesText() {
        GeminiResponse response = geminiClient.generate(
                GeminiRequest.text("Reply with exactly the word OK.")
        );

        assertThat(response.content()).isNotBlank();
        assertThat(response.model()).isNotBlank();
    }

    @Test
    void generatesStructuredJsonFromImage() {
        GeminiRequest request = new GeminiRequest(
                "Respond only with JSON that matches the supplied schema.",
                "Is an image attached?",
                List.of(new GeminiMedia("image/png", ONE_PIXEL_PNG)),
                true,
                """
                        {
                          "type": "object",
                          "properties": {
                            "imageAttached": {"type": "boolean"}
                          },
                          "required": ["imageAttached"]
                        }
                        """,
                null,
                0.0,
                128
        );

        GeminiResponse response = geminiClient.generate(request);

        assertThat(response.content()).contains("\"imageAttached\"");
    }
}
