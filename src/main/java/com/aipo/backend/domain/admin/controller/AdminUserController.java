package com.aipo.backend.domain.admin.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("userId", 1);
        item.put("loginId", "string");
        item.put("userName", "string");
        item.put("status", status);
        item.put("createdAt", Instant.now().toString());
        item.put("lastLoginAt", Instant.now().toString());

        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("page", page);
        pageData.put("size", size);
        pageData.put("totalElements", 1);
        pageData.put("totalPages", 1);
        pageData.put("items", List.of(item));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("data", pageData);
        res.put("error", null);
        return res;
    }
}
