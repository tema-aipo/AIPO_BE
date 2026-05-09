package com.aipo.backend.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SwaggerBasicAuthFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final byte[] expectedCredentialHash;

    public SwaggerBasicAuthFilter(boolean enabled, String username, String password) {
        this.enabled = enabled;
        this.expectedCredentialHash = sha256((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!enabled || !isSwaggerPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (!isAuthorized(authHeader)) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Swagger\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSwaggerPath(String path) {
        return "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/")
                || "/v3/api-docs".equals(path)
                || path.startsWith("/v3/api-docs/");
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            String encoded = authHeader.substring(6).trim();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            byte[] providedHash = sha256(decoded);
            return MessageDigest.isEqual(providedHash, expectedCredentialHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
