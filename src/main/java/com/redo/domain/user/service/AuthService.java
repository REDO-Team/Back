    package com.redo.domain.user.service;

    import com.redo.domain.user.dto.AuthResDTO;
    import com.redo.domain.user.entity.User;
    import com.redo.domain.user.entity.UserEmailVerification;
    import com.redo.domain.user.enums.UserProvider;
    import com.redo.domain.user.exception.AuthErrorCode;
    import com.redo.domain.user.converter.AuthConverter;
    import com.redo.domain.user.enums.UserStatus;
    import com.redo.domain.user.repository.UserRepository;
    import com.redo.global.apiPayload.exception.GeneralException;
    import com.redo.global.security.JwtUtil;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.redis.core.StringRedisTemplate;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import com.redo.domain.user.repository.UserEmailVerificationRepository;

    import java.time.Duration;
    import java.time.LocalDateTime;

    @Service
    @RequiredArgsConstructor
    public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final StringRedisTemplate redisTemplate;
        private final SocialOAuthService socialOAuthService;
        private final EmailService emailService;
        private final UserEmailVerificationRepository userEmailVerificationRepository;


        @Transactional
        public TokenResult login(String loginId, String password) {
            //id 로 유저찾기
            User user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_LOGIN_ID_OR_PASSWORD));
            //비번 검증 ( 원본 비밀번호와 암호화된 비밀번호)
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new GeneralException(AuthErrorCode.INVALID_LOGIN_ID_OR_PASSWORD);
            }
            //탈퇴한 계정인지 확인
            if (user.getStatus() == UserStatus.WITHDRAWN) {
                throw new GeneralException(AuthErrorCode.WITHDRAWN_ACCOUNT);
            }
            // 토큰 발급
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            redisTemplate.opsForValue().set(
                    "refresh:" + user.getId(),
                    refreshToken,
                    Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
    );

            return new TokenResult(user, accessToken, refreshToken, jwtUtil.getRefreshTokenExpiration());  // DTO 대신..
        }

        //Service ↔ Controller 사이에서만 쓰이는 내부용 객체 . DTO아님.
        public record TokenResult(
                User user,
                String accessToken,
                String refreshToken,
                long refreshTokenExpiration
        ) {}

        @Transactional
        public TokenResult reissue(String refreshToken){

            // 1. JWT 자체가 유효한지 확인
            if(!jwtUtil.validateToken(refreshToken)){
                throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);

            // 2. Redis에 저장된 값과 실제로 일치하는지 확인 (보완사항)
            String savedToken = redisTemplate.opsForValue().get("refresh:" + userId);
            if (savedToken == null || !savedToken.equals(refreshToken)) {
                throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            String newAccessToken = jwtUtil.generateAccessToken(userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId);

            // 4. Redis 값 교체 (기존 토큰 자동 무효화)
            redisTemplate.opsForValue().set(
                    "refresh:" + userId,
                    newRefreshToken,
                    Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
            );
            return new TokenResult(null, newAccessToken, newRefreshToken, jwtUtil.getRefreshTokenExpiration());
        }

        @Transactional
        public void logout(Long userId) {
            redisTemplate.delete("refresh:" + userId);
        }

        @Transactional
        public SocialLoginResult socialLogin(String provider, String socialAccessToken) {

            // 1. 구글/카카오에 물어봐서 사용자 정보 받기
            SocialOAuthService.SocialUserInfo userInfo;
            if (provider.equals("GOOGLE")) {
                userInfo = socialOAuthService.getGoogleUserInfo(socialAccessToken);
            } else if (provider.equals("KAKAO")) {
                userInfo = socialOAuthService.getKakaoUserInfo(socialAccessToken);
            } else {
                throw new GeneralException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            // 2. DB에서 기존 회원인지 확인
            UserProvider userProvider = UserProvider.valueOf(provider);
            return userRepository.findByProviderAndProviderUserId(userProvider, userInfo.socialId())
                    .map(user -> {
                        // 3-1. 기존 회원이면: 토큰 발급
                        String accessToken = jwtUtil.generateAccessToken(user.getId());
                        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

                        redisTemplate.opsForValue().set(
                                "refresh:" + user.getId(),
                                refreshToken,
                                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
                        );

                        return new SocialLoginResult(user, false, accessToken, refreshToken, null, null);
                    })
                    .orElseGet(() ->
                            // 3-2. 신규 회원이면: 토큰 없이 socialId만 반환
                            new SocialLoginResult(null, true, null, null, provider, userInfo.socialId())
                    );
        }

        // Service ↔ Controller 사이 내부용 객체
        public record SocialLoginResult(
                User user,
                Boolean isNewUser,
                String accessToken,
                String refreshToken,
                String socialProvider,
                String socialId
        ) {}

        //아이디 중복 확인 (회원가입)
        public AuthResDTO.LoginIdCheck checkLoginId(String loginId) {
            boolean exists = userRepository.findByLoginId(loginId).isPresent();

            return new AuthResDTO.LoginIdCheck(!exists);

        }

        // 이메일 주소 인증
        @Transactional
        public void sendVerificationEmail(String email) {

            // 1) 이미 가입된 이메일인지 확인
            userRepository.findByEmail(email).ifPresent(user -> {
                throw new GeneralException(AuthErrorCode.DUPLICATE_EMAIL);
            });

            // 2) 인증번호 생성
            String code = emailService.generateVerificationCode();

            // 3) 이메일 발송
            emailService.sendVerificationEmail(email, code);

            // 4) DB에 저장
            UserEmailVerification verification = UserEmailVerification.create(email, code);
            userEmailVerificationRepository.save(verification);
        }

        // 인증번호 확인
        @Transactional
        public void confirmVerificationCode(String email, String code) {

            // 1) 가장 최근 인증 기록 조회
            UserEmailVerification verification = userEmailVerificationRepository
                    .findTopByEmailOrderByCreatedAtDesc(email)
                    .orElseThrow(() -> new GeneralException(AuthErrorCode.VERIFICATION_NOT_FOUND));

            // 2) 만료 확인 (5분)
            if (verification.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                throw new GeneralException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
            }

            // 3) 인증번호 일치 확인
            if (!verification.getVerificationCode().equals(code)) {
                throw new GeneralException(AuthErrorCode.INVALID_VERIFICATION_CODE);
            }

            // 4) 인증 완료 처리
            verification.verify();
        }


    }