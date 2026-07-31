package com.redo.domain.certification.service.image;

import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.global.s3.config.S3Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3CertificationImageReaderTest {

    @Mock
    private S3Client s3Client;

    @Test
    void restoresImageBytesAndMimeTypeFromCertificationObjectKey() {
        S3CertificationImageReader reader = reader();
        byte[] bytes = new byte[]{1, 2, 3};
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder()
                                .contentType("image/jpeg")
                                .build(),
                        bytes
                ));

        CertificationImageContent content =
                reader.read("certifications/42/image.jpg");

        assertThat(content.mimeType()).isEqualTo("image/jpeg");
        assertThat(content.bytes()).containsExactly(bytes);
        ArgumentCaptor<GetObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key())
                .isEqualTo("certifications/42/image.jpg");
    }

    @Test
    void mapsS3ReadFailureToTypedCertificationError() {
        S3CertificationImageReader reader = reader();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.create("read failed"));

        assertThatThrownBy(() -> reader.read("certifications/42/image.jpg"))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.IMAGE_READ_FAILED);
    }

    private S3CertificationImageReader reader() {
        return new S3CertificationImageReader(
                s3Client,
                new S3Properties(
                        "test-bucket",
                        "ap-northeast-2",
                        DataSize.ofMegabytes(10),
                        Duration.ofMinutes(10)
                )
        );
    }
}
