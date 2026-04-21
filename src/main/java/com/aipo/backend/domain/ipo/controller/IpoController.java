package com.aipo.backend.domain.ipo.controller;

import com.aipo.backend.domain.ipo.dto.IpoDetailResponse;
import com.aipo.backend.domain.ipo.service.IpoService;
import com.aipo.backend.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ipos")
public class IpoController {

    private final IpoService ipoService;

    @GetMapping("/{ipoId}")
    public ResponseEntity<IpoDetailResponse> getIpoDetail(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, userId);
        return ResponseEntity.ok(response);
    }
}
