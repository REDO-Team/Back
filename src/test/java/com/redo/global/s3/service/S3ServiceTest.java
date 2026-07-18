package com.redo.global.s3.service;

import com.redo.global.s3.config.S3Properties;
import com.redo.global.s3.exception.S3Exception;
import com.redo.global.s3.exception.code.S3ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String BUCKET = "redo-images-test-ap-northeast-2-an";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties(
                BUCKET,
                "ap-northeast-2",
                DataSize.ofMegabytes(10),
                Duration.ofMinutes(10)
        );
        s3Service = new S3Service(s3Client, s3Presigner, properties);
    }

    @Test
    void uploadReturnsGeneratedObjectKey() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.JPG",
                "image/jpeg",
                "image-content".getBytes()
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = s3Service.upload(file, "/profiles/");

        assertThat(objectKey)
                .startsWith("profiles/")
                .endsWith(".jpg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(objectKey);
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void uploadRejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertThatThrownBy(() -> s3Service.upload(file, "community"))
                .isInstanceOf(S3Exception.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void uploadRejectsFileLargerThanConfiguredLimit() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(DataSize.ofMegabytes(10).toBytes() + 1);

        assertThatThrownBy(() -> s3Service.upload(file, "community"))
                .isInstanceOf(S3Exception.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    void uploadRejectsInvalidDirectory() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "content".getBytes()
        );

        assertThatThrownBy(() -> s3Service.upload(file, "../community"))
                .isInstanceOf(S3Exception.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.INVALID_DIRECTORY);
    }

    @Test
    void uploadAllReturnsEveryGeneratedObjectKey() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "first.jpg",
                "image/jpeg",
                "first".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "second.png",
                "image/png",
                "second".getBytes()
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        List<String> objectKeys = s3Service.uploadAll(List.of(firstFile, secondFile), "community");

        assertThat(objectKeys).hasSize(2);
        assertThat(objectKeys.get(0)).startsWith("community/").endsWith(".jpg");
        assertThat(objectKeys.get(1)).startsWith("community/").endsWith(".png");
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadAllDeletesPreviouslyUploadedObjectsWhenUploadFails() {
        MockMultipartFile validFile = new MockMultipartFile(
                "files",
                "first.jpg",
                "image/jpeg",
                "first".getBytes()
        );
        MockMultipartFile invalidFile = new MockMultipartFile(
                "files",
                "second.pdf",
                "application/pdf",
                "second".getBytes()
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertThatThrownBy(() -> s3Service.uploadAll(
                List.of(validFile, invalidFile),
                "community"
        ))
                .isInstanceOf(S3Exception.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.INVALID_IMAGE_TYPE);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteUsesConfiguredBucketAndObjectKey() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());
        String objectKey = "community/550e8400-e29b-41d4-a716-446655440000.webp";

        s3Service.delete(objectKey);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(objectKey);
    }

    @Test
    void deleteAllDeletesEveryObjectKey() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3Service.deleteAll(List.of(
                "community/first.jpg",
                "community/second.png"
        ));

        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteAllContinuesWhenOneDeleteFails() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.S3Exception.builder().message("delete failed").build())
                .thenReturn(DeleteObjectResponse.builder().build());

        assertThatThrownBy(() -> s3Service.deleteAll(List.of(
                "community/first.jpg",
                "community/second.png"
        )))
                .isInstanceOf(S3Exception.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.DELETE_FAILED);

        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void createPresignedUrlUsesConfiguredExpiration() {
        S3Properties properties = new S3Properties(
                BUCKET,
                "ap-northeast-2",
                DataSize.ofMegabytes(10),
                Duration.ofMinutes(10)
        );
        try (S3Presigner realPresigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build()) {
            S3Service service = new S3Service(s3Client, realPresigner, properties);

            String url = service.createPresignedUrl("profiles/profile-image.png");

            assertThat(url)
                    .contains(BUCKET)
                    .contains("profiles/profile-image.png")
                    .contains("X-Amz-Expires=600");
        }
    }
}
