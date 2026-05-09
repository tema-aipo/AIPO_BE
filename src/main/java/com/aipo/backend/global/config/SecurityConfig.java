package com.aipo.backend.global.config;

import com.aipo.backend.global.security.filter.SwaggerBasicAuthFilter;
import com.aipo.backend.global.security.jwt.JwtAuthenticationEntryPoint;
import com.aipo.backend.global.security.jwt.JwtAuthenticationFilter;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;
    @Value("${app.swagger.basic-auth.enabled:false}")
    private boolean swaggerBasicAuthEnabled;
    @Value("${app.swagger.basic-auth.username:}")
    private String swaggerBasicAuthUsername;
    @Value("${app.swagger.basic-auth.password:}")
    private String swaggerBasicAuthPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (isMissingRequiredSwaggerAuthCredentials()) {
            throw new IllegalStateException(
                    "Swagger Basic Auth is enabled but required properties are missing. " +
                            "Please set APP_SWAGGER_BASIC_AUTH_USERNAME and APP_SWAGGER_BASIC_AUTH_PASSWORD."
            );
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/v1/auth/**",
                                "/api/v1/admin/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new SwaggerBasicAuthFilter(
                                swaggerBasicAuthEnabled,
                                swaggerBasicAuthUsername,
                                swaggerBasicAuthPassword),
                        JwtAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private boolean isMissingRequiredSwaggerAuthCredentials() {
        return swaggerBasicAuthEnabled
                && (!StringUtils.hasText(swaggerBasicAuthUsername) || !StringUtils.hasText(swaggerBasicAuthPassword));
    }
}
// NOTE:
// 현재는 인증 기능 검증을 위한 최소 Security 설정이다.
// 추후 사용자 앱/관리자 웹/API 공개 범위에 따라
// permitAll 경로, USER/ADMIN 권한 정책, CORS, 인증 실패 응답 형식을 세분화해야 한다.
