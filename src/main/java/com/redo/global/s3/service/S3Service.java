package com.redo.global.s3.service;

import com.redo.global.s3.config.S3Properties;
import com.redo.global.s3.exception.S3Exception;
import com.redo.global.s3.exception.code.S3ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Pattern DIRECTORY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9/_-]*$");
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9/_\\-.]*$");
    private static final Map<String, Set<String>> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg", Set.of("image/jpeg", "image/jpg"),
            "jpeg", Set.of("image/jpeg", "image/jpg"),
            "png", Set.of("image/png"),
            "webp", Set.of("image/webp"),
            "gif", Set.of("image/gif")
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    // 단일 이미지 업로드 및 객체 키 생성 로직
    public String upload(MultipartFile file, String directory) {
        validateFile(file);
        String normalizedDirectory = normalizeDirectory(directory);
        String extension = extractExtension(file.getOriginalFilename());
        validateImageType(extension, file.getContentType());

        String objectKey = normalizedDirectory + "/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return objectKey;
        } catch (IOException | SdkException exception) {
            log.error("Failed to upload image to S3. key={}", objectKey, exception);
            throw new S3Exception(S3ErrorCode.UPLOAD_FAILED);
        }
    }

    // 다중 이미지 업로드 및 부분 업로드 실패 시 롤백 로직
    public List<String> uploadAll(List<MultipartFile> files, String directory) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedKeys.add(upload(file, directory));
            }
            return List.copyOf(uploadedKeys);
        } catch (S3Exception exception) {
            rollbackUploadedObjects(uploadedKeys);
            throw exception;
        }
    }

    // 객체 키를 이용한 단일 이미지 삭제 로직
    public void delete(String objectKey) {
        validateObjectKey(objectKey);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            log.error("Failed to delete image from S3. key={}", objectKey, exception);
            throw new S3Exception(S3ErrorCode.DELETE_FAILED);
        }
    }

    // 다중 이미지 삭제 및 일부 실패 시 나머지 삭제 계속 처리 로직
    public void deleteAll(Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        objectKeys.forEach(this::validateObjectKey);

        boolean deleteFailed = false;
        for (String objectKey : objectKeys) {
            try {
                delete(objectKey);
            } catch (S3Exception exception) {
                deleteFailed = true;
            }
        }

        if (deleteFailed) {
            throw new S3Exception(S3ErrorCode.DELETE_FAILED);
        }
    }

    // 비공개 이미지 조회를 위한 Presigned URL 생성 로직
    public String createPresignedUrl(String objectKey) {
        validateObjectKey(objectKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
        } catch (SdkException | IllegalArgumentException exception) {
            log.error("Failed to generate presigned S3 URL. key={}", objectKey, exception);
            throw new S3Exception(S3ErrorCode.URL_GENERATION_FAILED);
        }
    }

    // 빈 파일 및 최대 파일 크기 검증 로직
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new S3Exception(S3ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new S3Exception(S3ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // 도메인별 S3 디렉터리 경로 정규화 및 검증 로직
    private String normalizeDirectory(String directory) {
        if (directory == null) {
            throw new S3Exception(S3ErrorCode.INVALID_DIRECTORY);
        }

        String normalized = directory.strip().replaceAll("^/+|/+$", "");
        if (!DIRECTORY_PATTERN.matcher(normalized).matches()
                || normalized.contains("//")
                || normalized.contains("..")) {
            throw new S3Exception(S3ErrorCode.INVALID_DIRECTORY);
        }
        return normalized;
    }

    // 원본 파일명에서 허용된 이미지 확장자 추출 로직
    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new S3Exception(S3ErrorCode.INVALID_IMAGE_TYPE);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.containsKey(extension)) {
            throw new S3Exception(S3ErrorCode.INVALID_IMAGE_TYPE);
        }
        return extension;
    }

    // 파일 확장자와 Content-Type 일치 여부 검증 로직
    private void validateImageType(String extension, String contentType) {
        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.get(extension).contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new S3Exception(S3ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    // 삭제 및 URL 생성에 사용되는 S3 객체 키 검증 로직
    private void validateObjectKey(String objectKey) {
        if (objectKey == null
                || !OBJECT_KEY_PATTERN.matcher(objectKey).matches()
                || objectKey.contains("//")
                || objectKey.contains("..")) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }
    }

    // 다중 업로드 실패 시 이미 업로드된 객체 정리 로직
    private void rollbackUploadedObjects(Collection<String> uploadedKeys) {
        try {
            deleteAll(uploadedKeys);
        } catch (S3Exception exception) {
            log.error("Failed to roll back partially uploaded S3 images. keys={}", uploadedKeys, exception);
        }
    }
}
