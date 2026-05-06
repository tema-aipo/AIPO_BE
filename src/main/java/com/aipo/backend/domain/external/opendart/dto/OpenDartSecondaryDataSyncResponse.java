package com.aipo.backend.domain.external.opendart.dto;

public record OpenDartSecondaryDataSyncResponse(
        int targetStockCount,
        int cachedCompanyOverviewResponseCount,
        int fetchedCompanyOverviewResponseCount,
        int upsertedCompanyProfileCount,
        int supplementedStockCount,
        int failedStockCount
) {
}
