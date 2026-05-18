package com.aipo.backend.domain.ipo.repository;

import java.math.BigDecimal;

public interface IpoDetailProjection {

    Long getStockId();

    String getCompanyName();

    String getStockName();

    String getCorpName();

    String getStockCode();

    Integer getOfferingPrice();

    BigDecimal getConfirmedOfferPrice();

    Float getAttractScore();

    Integer getRecentGrowthScore();

    String getMarketType();

    String getOneLineDescription();

    String getUnderwriter();

    String getSubscriptionDate();

    String getDemandForecastDate();

    String getRefundDate();

    String getListingDate();

    String getSubscriptionStartDate();

    String getSubscriptionEndDate();
}
