package com.aipo.backend.domain.home.controller;

import com.aipo.backend.domain.home.dto.HomeResponse;
import com.aipo.backend.domain.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // JSON 응답을 반환하는 REST API 컨트롤러
@RequiredArgsConstructor // final 필드 생성자 자동 생성
@RequestMapping("/api/v1/home") // 홈 API 기본 경로
public class HomeController {

    private final HomeService homeService;

    @GetMapping // GET /api/home
    public ResponseEntity<HomeResponse> getHome(
            // tab이 없으면 기본값 recentGrowth 사용
            @RequestParam(required = false, defaultValue = "recentGrowth") String tab
    ) {
        // 서비스에 tab을 넘겨서 홈 데이터 생성
        HomeResponse response = homeService.getHome(tab);

        // 200 OK + 응답 바디 반환
        return ResponseEntity.ok(response);
    }
}