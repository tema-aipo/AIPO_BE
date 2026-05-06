package com.aipo.backend.domain.external.opendart.dto;

import java.time.LocalDate;

public record OpenDartIpoSyncResponse(
        LocalDate startDate,
        LocalDate endDate,
        int searchedDisclosureCount,
        int newDisclosureCount,
        int cachedSearchResponseCount,
        int fetchedSearchResponseCount,
        int cachedDetailResponseCount,
        int fetchedDetailResponseCount,
        int cachedCompanyOverviewResponseCount,
        int fetchedCompanyOverviewResponseCount,
        int processedDisclosureCount,
        int fallbackStockCount,
        int upsertedStockCount,
        int failedDisclosureCount,
        int kindCachedResponseCount,
        int kindFetchedResponseCount,
        int kindMatchedStockCount,
        int kindSupplementedStockCount,
        int kindSupplementedScheduleCount
) {
}
