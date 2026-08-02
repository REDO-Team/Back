package com.redo.domain.user.dto;

import com.redo.domain.user.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class ProfileReqDTO {

    public record UpdateNickname(
            @NotBlank
            @Pattern(regexp = "^[가-힣0-9]{2,10}$", message = "닉네임 형식에 맞게 입력해주세요.")
            String nickname
    ) {}

    public record CreateProfile(
            @NotBlank
            @Pattern(regexp = "^[가-힣0-9]{2,10}$", message = "닉네임 형식에 맞게 입력해주세요.")
            String nickname,

            String characterCode,

            Gender gender,

            LocalDate birthDate
    ) {}

    public record UpdateCharacter(
            @NotBlank
            String characterCode
    ) {}

}