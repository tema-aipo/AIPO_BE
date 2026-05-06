package com.aipo.backend.domain.external.opendart.service;

import com.aipo.backend.domain.external.opendart.config.OpenDartProperties;
import com.aipo.backend.domain.external.opendart.dto.OpenDartCombinedIpoSyncResponse;
import com.aipo.backend.domain.external.opendart.dto.OpenDartIpoSyncResponse;
import com.aipo.backend.domain.external.opendart.dto.OpenDartSecondaryDataSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenDartCombinedIpoSyncService {

    private final OpenDartProperties properties;
    private final OpenDartIpoSyncService openDartIpoSyncService;
    private final OpenDartSecondaryDataSyncService openDartSecondaryDataSyncService;

    public OpenDartCombinedIpoSyncResponse syncConfiguredPeriod() {
        int collectionMonths = properties.collectionMonths();
        return sync(collectionMonths);
    }

    public OpenDartCombinedIpoSyncResponse sync(int collectionMonths) {
        int normalizedMonths = Math.max(1, collectionMonths);
        OpenDartIpoSyncResponse primarySync = openDartIpoSyncService.syncRecentMonths(normalizedMonths);
        OpenDartSecondaryDataSyncResponse secondarySync = openDartSecondaryDataSyncService.syncCompanyProfiles();
        return new OpenDartCombinedIpoSyncResponse(normalizedMonths, primarySync, secondarySync);
    }
}
