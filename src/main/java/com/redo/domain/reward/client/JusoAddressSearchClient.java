package com.redo.domain.reward.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.redo.domain.reward.dto.res.ShippingAddressCandidateResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressSearchResponseDTO;
import com.redo.domain.reward.exception.ShippingAddressException;
import com.redo.domain.reward.exception.code.ShippingAddressErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class JusoAddressSearchClient {

    private static final String SUCCESS_ERROR_CODE = "0";

    private final WebClient webClient;
    private final String addressSearchUrl;
    private final String confirmationKey;

    public JusoAddressSearchClient(
            WebClient.Builder webClientBuilder,
            @Value("${juso.address-search-url}") String addressSearchUrl,
            @Value("${juso.confirmation-key:}") String confirmationKey
    ) {
        this.webClient = webClientBuilder.build();
        this.addressSearchUrl = addressSearchUrl;
        this.confirmationKey = confirmationKey;
    }

    public ShippingAddressSearchResponseDTO search(String keyword, int page, int size) {
        validateJusoConfig();

        URI uri = UriComponentsBuilder.fromUriString(addressSearchUrl)
                .queryParam("confmKey", confirmationKey)
                .queryParam("currentPage", page)
                .queryParam("countPerPage", size)
                .queryParam("keyword", keyword)
                .queryParam("resultType", "json")
                .build()
                .encode()
                .toUri();

        try {
            JsonNode response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseSearchResponse(response, page, size);
        } catch (WebClientException e) {
            throw new ShippingAddressException(ShippingAddressErrorCode.ADDRESS_SEARCH_FAILED);
        }
    }

    private void validateJusoConfig() {
        if (confirmationKey == null || confirmationKey.isBlank()) {
            throw new ShippingAddressException(ShippingAddressErrorCode.ADDRESS_SEARCH_FAILED);
        }
    }

    private ShippingAddressSearchResponseDTO parseSearchResponse(
            JsonNode response,
            int requestedPage,
            int requestedSize
    ) {
        if (response == null || !response.path("results").isObject()) {
            throw new ShippingAddressException(ShippingAddressErrorCode.ADDRESS_SEARCH_FAILED);
        }

        JsonNode results = response.path("results");
        JsonNode common = results.path("common");
        String errorCode = textOrNull(common.path("errorCode"));

        if (!SUCCESS_ERROR_CODE.equals(errorCode)) {
            throw new ShippingAddressException(ShippingAddressErrorCode.ADDRESS_SEARCH_FAILED);
        }

        List<ShippingAddressCandidateResponseDTO> addressCandidates = parseAddressItems(results.path("juso"));
        int totalCount = intOrDefault(common.path("totalCount"), 0);
        int page = intOrDefault(common.path("currentPage"), requestedPage);
        int size = intOrDefault(common.path("countPerPage"), requestedSize);
        boolean hasNext = (long) page * size < totalCount;

        return new ShippingAddressSearchResponseDTO(
                addressCandidates,
                page,
                size,
                totalCount,
                hasNext
        );
    }

    private List<ShippingAddressCandidateResponseDTO> parseAddressItems(JsonNode juso) {
        List<ShippingAddressCandidateResponseDTO> items = new ArrayList<>();

        if (!juso.isArray()) {
            return items;
        }

        for (JsonNode address : juso) {
            items.add(new ShippingAddressCandidateResponseDTO(
                    firstNonBlank(address.path("roadAddrPart1"), address.path("roadAddr")),
                    textOrNull(address.path("jibunAddr")),
                    textOrNull(address.path("zipNo")),
                    textOrNull(address.path("bdNm"))
            ));
        }

        return items;
    }

    private String firstNonBlank(JsonNode primary, JsonNode fallback) {
        String value = textOrNull(primary);
        return value != null ? value : textOrNull(fallback);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private int intOrDefault(JsonNode node, int defaultValue) {
        String value = textOrNull(node);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
