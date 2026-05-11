package com.aipo.backend.domain.log.service;

import com.aipo.backend.domain.log.entity.UserLoginLog;
import com.aipo.backend.domain.log.repository.UserLoginLogRepository;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginLogService {

    private final UserLoginLogRepository loginLogRepository;

    public void record(User user) {
        loginLogRepository.save(new UserLoginLog(user));
    }

    @Transactional(readOnly = true)
    public Page<LoginLogResponse> getLogs(String loginId, UserRole role,
                                          LocalDateTime from, LocalDateTime to,
                                          Pageable pageable) {
        return loginLogRepository.findAllByFilter(loginId, role, from, to, pageable)
                .map(LoginLogResponse::from);
    }

    @Getter
    @AllArgsConstructor
    public static class LoginLogResponse {
        private Long logId;
        private String loginId;
        private UserRole role;
        private LocalDateTime loggedInAt;

        public static LoginLogResponse from(UserLoginLog log) {
            return new LoginLogResponse(
                    log.getLogId(),
                    log.getLoginId(),
                    log.getRole(),
                    log.getLoggedInAt()
            );
        }
    }
}
