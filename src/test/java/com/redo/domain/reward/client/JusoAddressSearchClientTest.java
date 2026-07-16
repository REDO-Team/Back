package com.redo.domain.reward.client;

import com.redo.domain.reward.dto.res.ShippingAddressCandidateResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressSearchResponseDTO;
import com.redo.domain.reward.exception.ShippingAddressException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JusoAddressSearchClientTest {

    @Test
    void constructorFailsFastWhenConfirmationKeyIsBlank() {
        assertThatThrownBy(() -> new JusoAddressSearchClient(
                WebClient.builder(),
                "https://business.juso.go.kr/addrlink/addrLinkApi.do",
                " ",
                5_000
        )).isInstanceOf(ShippingAddressException.class);
    }

    @Test
    void searchMapsJusoResponseAndPagination() {
        String responseBody = """
                {
                  "results": {
                    "common": {
                      "errorCode": "0",
                      "errorMessage": "정상",
                      "totalCount": "21",
                      "currentPage": "1",
                      "countPerPage": "20"
                    },
                    "juso": [
                      {
                        "roadAddr": "인천광역시 부평구 장제로159번길 7 (부평동)",
                        "roadAddrPart1": "인천광역시 부평구 장제로159번길 7",
                        "jibunAddr": "인천광역시 부평구 부평동 377-14",
                        "zipNo": "21360",
                        "bdNm": ""
                      },
                      {
                        "roadAddr": "인천광역시 부평구 장제로159번길 8 (부평동, 테스트빌딩)",
                        "roadAddrPart1": "인천광역시 부평구 장제로159번길 8",
                        "jibunAddr": "인천광역시 부평구 부평동 377-15",
                        "zipNo": "21360",
                        "bdNm": "테스트빌딩"
                      }
                    ]
                  }
                }
                """;
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                                .body(responseBody)
                                .build()
                ));
        JusoAddressSearchClient client = new JusoAddressSearchClient(
                webClientBuilder,
                "https://business.juso.go.kr/addrlink/addrLinkApi.do",
                "test-confirmation-key",
                5_000
        );

        ShippingAddressSearchResponseDTO response = client.search("장제로159번길", 1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalCount()).isEqualTo(21);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.addressCandidates()).containsExactly(
                new ShippingAddressCandidateResponseDTO(
                        "인천광역시 부평구 장제로159번길 7",
                        "인천광역시 부평구 부평동 377-14",
                        "21360",
                        null
                ),
                new ShippingAddressCandidateResponseDTO(
                        "인천광역시 부평구 장제로159번길 8",
                        "인천광역시 부평구 부평동 377-15",
                        "21360",
                        "테스트빌딩"
                )
        );
    }

    @Test
    void searchConvertsDecodingFailureToShippingAddressException() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                                .body("{invalid-json")
                                .build()
                ));
        JusoAddressSearchClient client = new JusoAddressSearchClient(
                webClientBuilder,
                "https://business.juso.go.kr/addrlink/addrLinkApi.do",
                "test-confirmation-key",
                5_000
        );

        assertThatThrownBy(() -> client.search("장제로159번길", 1, 20))
                .isInstanceOf(ShippingAddressException.class);
    }

    @Test
    void searchConvertsTimeoutToShippingAddressException() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.never());
        JusoAddressSearchClient client = new JusoAddressSearchClient(
                webClientBuilder,
                "https://business.juso.go.kr/addrlink/addrLinkApi.do",
                "test-confirmation-key",
                10
        );

        assertThatThrownBy(() -> client.search("장제로159번길", 1, 20))
                .isInstanceOf(ShippingAddressException.class);
    }
}
