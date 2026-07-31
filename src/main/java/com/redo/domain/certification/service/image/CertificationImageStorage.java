package com.redo.domain.certification.service.image;

import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class CertificationImageStorage {

    private final S3Service s3Service;

    public String upload(Long userId, MultipartFile image) {
        return s3Service.upload(image, "certifications/" + userId);
    }

    public void delete(String imageKey) {
        s3Service.delete(imageKey);
    }
}
