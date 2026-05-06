package com.aipo.backend.domain.external.opendart.controller;

import com.aipo.backend.domain.external.opendart.dto.OpenDartCombinedIpoSyncResponse;
import com.aipo.backend.domain.external.opendart.dto.OpenDartIpoSyncResponse;
import com.aipo.backend.domain.external.opendart.dto.OpenDartSecondaryDataSyncResponse;
import com.aipo.backend.domain.external.opendart.service.OpenDartCombinedIpoSyncService;
import com.aipo.backend.domain.external.opendart.service.OpenDartIpoSyncService;
import com.aipo.backend.domain.external.opendart.service.OpenDartSecondaryDataSyncService;
import com.aipo.backend.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/external/ipos")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@Tag(name = "Admin External IPO Sync", description = "Admin external IPO data sync API")
public class AdminOpenDartIpoSyncController {

    private final OpenDartIpoSyncService openDartIpoSyncService;
    private final OpenDartSecondaryDataSyncService openDartSecondaryDataSyncService;
    private final OpenDartCombinedIpoSyncService openDartCombinedIpoSyncService;

    @PostMapping("/sync")
    @Operation(summary = "OpenDART IPO primary and secondary data sync")
    public ResponseEntity<OpenDartCombinedIpoSyncResponse> syncAll() {
        return ResponseEntity.ok(openDartCombinedIpoSyncService.syncConfiguredPeriod());
    }

    @PostMapping("/primary/sync")
    @Operation(summary = "OpenDART IPO primary disclosure data sync")
    public ResponseEntity<OpenDartIpoSyncResponse> syncPrimary() {
        return ResponseEntity.ok(openDartIpoSyncService.syncRecentMonth());
    }

    @PostMapping("/secondary-data/sync")
    @Operation(summary = "OpenDART IPO secondary company data sync")
    public ResponseEntity<OpenDartSecondaryDataSyncResponse> syncSecondaryData() {
        return ResponseEntity.ok(openDartSecondaryDataSyncService.syncCompanyProfiles());
    }
}
