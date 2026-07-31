package com.redo.domain.certification.service.image;

import com.redo.global.s3.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationImageStorageTest {

    @Mock
    private S3Service s3Service;

    @Test
    void storesCertificationImageUnderUserDirectoryAndReturnsObjectKey() {
        CertificationImageStorage storage = new CertificationImageStorage(s3Service);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "can.jpg",
                "image/jpeg",
                "image".getBytes()
        );
        String expectedKey = "certifications/42/generated.jpg";
        when(s3Service.upload(image, "certifications/42")).thenReturn(expectedKey);

        String imageKey = storage.upload(42L, image);

        assertThat(imageKey).isEqualTo(expectedKey);
        verify(s3Service).upload(image, "certifications/42");
    }

    @Test
    void delegatesCompensationDelete() {
        CertificationImageStorage storage = new CertificationImageStorage(s3Service);

        storage.delete("certifications/42/generated.jpg");

        verify(s3Service).delete("certifications/42/generated.jpg");
    }
}
