package com.aipo.backend.domain.external.opendart.dto;

public record OpenDartCombinedIpoSyncResponse(
        int collectionMonths,
        OpenDartIpoSyncResponse primarySync,
        OpenDartSecondaryDataSyncResponse secondarySync
) {
}
