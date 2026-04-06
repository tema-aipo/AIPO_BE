package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final LoginLogService loginLogService;

    @GetMapping("/login")
    public Page<LoginLogService.LoginLogResponse> loginLogs(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loggedInAt"));
        return loginLogService.getLogs(loginId, role, from, to, pageable);
    }
}
