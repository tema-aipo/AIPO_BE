package com.aipo.backend.domain.admin.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("from", from.toString());
        range.put("to", to.toString());

        Map<String, Object> users = new LinkedHashMap<>();
        users.put("total", 0);
        users.put("new", 0);
        users.put("active", 0);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("loginSuccess", 0);
        auth.put("loginFail", 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("range", range);
        data.put("users", users);
        data.put("auth", auth);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("data", data);
        res.put("error", null);
        return res;
    }
}
