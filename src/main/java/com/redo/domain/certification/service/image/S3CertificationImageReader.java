package com.redo.domain.certification.service.image;

import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.global.s3.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import static com.redo.domain.certification.exception.code.CertificationErrorCode.IMAGE_READ_FAILED;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3CertificationImageReader implements CertificationImageReader {

    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public CertificationImageContent read(String imageKey) {
        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(imageKey)
                            .build()
            );
            return new CertificationImageContent(
                    resolveMimeType(object.response().contentType(), imageKey),
                    object.asByteArray()
            );
        } catch (SdkException | IllegalArgumentException exception) {
            log.error(
                    "Failed to read certification image from S3. key={}",
                    imageKey,
                    exception
            );
            throw new CertificationException(
                    IMAGE_READ_FAILED,
                    CertificationErrorDetail.type("IMAGE_READ_FAILED")
            );
        }
    }

    private String resolveMimeType(String contentType, String imageKey) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String lowerKey = imageKey.toLowerCase();
        if (lowerKey.endsWith(".png")) {
            return "image/png";
        }
        if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
