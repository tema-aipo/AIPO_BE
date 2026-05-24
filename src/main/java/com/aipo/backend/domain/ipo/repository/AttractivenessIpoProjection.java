package com.aipo.backend.domain.ipo.repository;

public interface AttractivenessIpoProjection {

    Long getStockId();

    String getCorpName();

    String getCompetitionRatio();

    String getInstCommitmentRatio();

    String getFloatingStockRatio();

    String getLockupTotalRatio();
}
