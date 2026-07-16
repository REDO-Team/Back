package com.redo.domain.reward.client;

import com.redo.domain.reward.dto.res.ShippingAddressCandidateResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressSearchResponseDTO;
import com.redo.domain.reward.exception.ShippingAddressException;
import com.redo.domain.reward.exception.code.ShippingAddressErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JusoAddressSearchClientTest {

    private static final String ADDRESS_SEARCH_URL =
            "https://business.juso.go.kr/addrlink/addrLinkApi.do";
    private static final long DEFAULT_TIMEOUT_MILLIS = 5_000;

    @Test
    void constructorFailsFastWhenConfirmationKeyIsBlank() {
        assertThatThrownBy(() -> new JusoAddressSearchClient(
                WebClient.builder(),
                ADDRESS_SEARCH_URL,
                " ",
                DEFAULT_TIMEOUT_MILLIS
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
        JusoAddressSearchClient client = createClient(responseBody);

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
    void searchConvertsJusoErrorCodeToAddressSearchFailed() {
        String responseBody = """
                {
                  "results": {
                    "common": {
                      "errorCode": "E0005",
                      "errorMessage": "검색어가 입력되지 않았습니다.",
                      "totalCount": "0",
                      "currentPage": "1",
                      "countPerPage": "20"
                    },
                    "juso": []
                  }
                }
                """;
        JusoAddressSearchClient client = createClient(responseBody);

        assertThatThrownBy(() -> client.search("장제로159번길", 1, 20))
                .isInstanceOfSatisfying(ShippingAddressException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ShippingAddressErrorCode.ADDRESS_SEARCH_FAILED)
                );
    }

    @Test
    void searchUsesRoadAddressWhenRoadAddressPartOneIsBlank() {
        String responseBody = """
                {
                  "results": {
                    "common": {
                      "errorCode": "0",
                      "errorMessage": "정상",
                      "totalCount": "1",
                      "currentPage": "1",
                      "countPerPage": "20"
                    },
                    "juso": [
                      {
                        "roadAddr": "인천광역시 부평구 장제로159번길 7 (부평동)",
                        "roadAddrPart1": "",
                        "jibunAddr": "인천광역시 부평구 부평동 377-14",
                        "zipNo": "21360",
                        "bdNm": ""
                      }
                    ]
                  }
                }
                """;
        JusoAddressSearchClient client = createClient(responseBody);

        ShippingAddressSearchResponseDTO response = client.search("장제로159번길", 1, 20);

        assertThat(response.addressCandidates()).singleElement()
                .extracting(ShippingAddressCandidateResponseDTO::roadAddress)
                .isEqualTo("인천광역시 부평구 장제로159번길 7 (부평동)");
    }

    @Test
    void searchConvertsDecodingFailureToShippingAddressException() {
        JusoAddressSearchClient client = createClient("{invalid-json");

        assertThatThrownBy(() -> client.search("장제로159번길", 1, 20))
                .isInstanceOf(ShippingAddressException.class);
    }

    @Test
    void searchConvertsTimeoutToShippingAddressException() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.never());
        JusoAddressSearchClient client = new JusoAddressSearchClient(
                webClientBuilder,
                ADDRESS_SEARCH_URL,
                "test-confirmation-key",
                10
        );

        assertThatThrownBy(() -> client.search("장제로159번길", 1, 20))
                .isInstanceOf(ShippingAddressException.class);
    }

    private JusoAddressSearchClient createClient(String responseBody) {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                                .body(responseBody)
                                .build()
                ));

        return new JusoAddressSearchClient(
                webClientBuilder,
                ADDRESS_SEARCH_URL,
                "test-confirmation-key",
                DEFAULT_TIMEOUT_MILLIS
        );
    }
}
