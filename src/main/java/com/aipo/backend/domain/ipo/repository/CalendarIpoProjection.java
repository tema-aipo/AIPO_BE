package com.aipo.backend.domain.ipo.repository;

import java.time.LocalDate;

public interface CalendarIpoProjection {

    Long getStockId();

    String getCorpName();

    String getStockCode();

    Float getAttractScore();

    String getDemandForecastDate();

    String getSubscriptionDate();

    String getRefundDate();

    LocalDate getListingDate();
}
