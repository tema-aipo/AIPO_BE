package com.aipo.backend.domain.external.kind.dto;

public record KindIpoSupplementResult(
        int cachedResponseCount,
        int fetchedResponseCount,
        int matchedStockCount,
        int supplementedStockCount,
        int supplementedScheduleCount
) {
    public static KindIpoSupplementResult empty() {
        return new KindIpoSupplementResult(0, 0, 0, 0, 0);
    }
}
