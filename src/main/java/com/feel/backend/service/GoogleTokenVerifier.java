package com.feel.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * Google ID 토큰 검증 (Google API Client 라이브러리 사용).
 * 프론트엔드 GIS / OAuth 클라이언트가 발급한 ID 토큰의 서명·만료·aud를 검증한다.
 */
@Component
@Slf4j
public class GoogleTokenVerifier {

    public static final String ALLOWED_DOMAIN = "@jbnu.ac.kr";

    @Value("${app.google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );
        if (StringUtils.hasText(googleClientId)) {
            builder.setAudience(Collections.singletonList(googleClientId.trim()));
        } else {
            log.warn("app.google.client-id 가 비어 있습니다. aud 검증을 건너뜁니다. 로컬 외에는 반드시 설정하세요.");
        }
        this.verifier = builder.build();
    }

    public GoogleTokenInfo verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new RuntimeException("유효하지 않은 Google 로그인 정보입니다.");
        }
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new RuntimeException("유효하지 않은 Google 로그인 정보입니다.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new RuntimeException("이메일이 인증되지 않은 Google 계정입니다.");
            }

            GoogleTokenInfo info = new GoogleTokenInfo();
            info.setEmail(payload.getEmail());
            info.setEmailVerified(String.valueOf(payload.getEmailVerified()));
            info.setAud(payload.getAudience() != null ? payload.getAudience().toString() : null);
            info.setSub(payload.getSubject());
            info.setName((String) payload.get("name"));
            return info;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID 토큰 검증 실패: {}", e.getMessage());
            throw new RuntimeException("유효하지 않은 Google 로그인 정보입니다.");
        }
    }

    public static boolean isAllowedEmail(String email) {
        return email != null && email.toLowerCase().endsWith(ALLOWED_DOMAIN);
    }

    @Data
    public static class GoogleTokenInfo {
        private String email;
        private String emailVerified;
        private String aud;
        private String sub;
        private String name;
    }
}
