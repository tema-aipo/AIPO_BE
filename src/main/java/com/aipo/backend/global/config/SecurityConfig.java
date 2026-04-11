package com.aipo.backend.global.config;

import com.aipo.backend.global.security.jwt.JwtAuthenticationFilter;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT 생성/검증에 사용하는 Provider
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API 서버이므로 기본 CSRF 보호는 비활성화
                // 현재는 세션을 쓰지 않고 JWT 기반으로 동작하기 때문에 꺼두는 것이 일반적
                .csrf(csrf -> csrf.disable())

                // 기본 로그인 폼 비활성화
                // 우리는 직접 만든 로그인 API(/api/v1/auth/**)를 사용함
                .formLogin(form -> form.disable())

                // HTTP Basic 인증 비활성화
                // Authorization 헤더의 JWT만 사용할 예정
                .httpBasic(basic -> basic.disable())

                // 세션을 사용하지 않는 Stateless 방식으로 설정
                // 서버가 로그인 상태를 저장하지 않고, 매 요청마다 JWT를 검증함
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // 헬스체크 API는 누구나 호출 가능
                                "/api/health",

                                // 로그인/회원가입/토큰 재발급 등 인증 관련 API는 공개
                                "/api/v1/auth/**",

                                // Swagger UI 관련 경로 공개
                                "/swagger-ui/**",

                                // OpenAPI 문서 경로 공개
                                "/v3/api-docs/**",

                                // 홈 API 공개
                                // 현재는 메인 화면 진입 시 비로그인 상태에서도 볼 수 있도록 허용
                                "/api/v1/home/**",

                                // 공모주 목록/상세 API 공개
                                // Swagger 테스트 및 일반 사용자 공개 조회를 위해 허용
                                "/api/v1/ipos/**"
                        ).permitAll()

                        // 관리자 API는 ADMIN 권한만 접근 가능
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 사용자 전용 API는 USER 또는 ADMIN 권한 필요
                        .requestMatchers("/api/v1/users/**").hasAnyRole("USER", "ADMIN")

                        // 위에서 허용하지 않은 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter보다 먼저 실행
                // 요청 헤더의 JWT를 먼저 검사해서 SecurityContext에 사용자 정보를 넣음
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // AuthenticationManager를 Bean으로 등록해서 로그인 서비스에서 사용할 수 있게 함
        return configuration.getAuthenticationManager();
    }

    /*
     * =========================
     * 나중에 바꿔야 할 가능성이 있는 부분
     * =========================
     *
     * 1. /api/v1/home/** 공개 여부
     *    - 지금은 비로그인 사용자도 홈 화면을 볼 수 있도록 permitAll로 열어둠
     *    - 나중에 개인화 홈(관심종목 기반 추천, 사용자 맞춤 점수)으로 바뀌면
     *      authenticated()로 바꾸거나 일부만 인증 필요로 나눌 수 있음
     *
     * 2. /api/v1/ipos/** 공개 여부
     *    - 지금은 공모주 목록/상세를 공개 조회로 가정하고 permitAll로 열어둠
     *    - 추후 로그인 사용자만 접근 가능하게 정책이 바뀌면 권한 제한 필요
     *
     * 3. Swagger 경로 공개 여부
     *    - 개발 단계에서는 Swagger를 permitAll로 열어두는 게 편함
     *    - 운영 환경에서는 Swagger 접근을 막거나 관리자만 허용하도록 바꾸는 것이 일반적
     *
     * 4. requestMatchers 경로 세분화
     *    - 나중에 홈 API 중 일부는 공개, 일부는 인증 필요로 나눌 수 있음
     *    - 예: /api/v1/home/public, /api/v1/home/private
     *
     * 5. JWT 필터 예외 경로 처리
     *    - 현재는 permitAll이어도 필터를 통과함
     *    - 필요하면 JwtAuthenticationFilter 내부에서 Swagger/공개 API는
     *      토큰 검사 로직을 더 가볍게 처리하도록 최적화할 수 있음
     *
     * 6. CORS 설정
     *    - 프론트와 백엔드가 다른 도메인/포트에서 동작하면
     *      SecurityConfig 또는 별도 CorsConfig에서 CORS 설정 추가 필요
     */
}