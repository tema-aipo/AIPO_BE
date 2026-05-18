package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.ipo.entity.IpoStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IpoStockViewMapper {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})|(?<!\\d)(\\d{1,2})\\.(\\d{1,2})(?!\\d)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.M.d");

    private IpoStockViewMapper() {
    }

    public static String displayName(IpoStock stock) {
        return displayName(stock.getCompanyName(), stock.getStockName(), stock.getCorpName());
    }

    public static String displayName(String companyName, String stockName, String corpName) {
        return firstText(companyName, stockName, corpName, "");
    }

    public static String displayStockName(IpoStock stock) {
        return firstText(stock.getStockName(), stock.getCompanyName(), stock.getCorpName(), "");
    }

    public static String displayCompanyName(IpoStock stock) {
        return firstText(stock.getCompanyName(), stock.getStockName(), stock.getCorpName(), "");
    }

    public static String displayCompanyName(String companyName, String stockName, String corpName) {
        return firstText(companyName, stockName, corpName, "");
    }

    public static BigDecimal offerPrice(IpoStock stock) {
        if (stock.getConfirmedOfferPrice() != null) {
            return stock.getConfirmedOfferPrice();
        }
        if (stock.getOfferingPrice() != null) {
            return BigDecimal.valueOf(stock.getOfferingPrice());
        }
        return null;
    }

    public static BigDecimal offerPrice(BigDecimal confirmedOfferPrice, Integer offeringPrice) {
        if (confirmedOfferPrice != null) {
            return confirmedOfferPrice;
        }
        if (offeringPrice != null) {
            return BigDecimal.valueOf(offeringPrice);
        }
        return null;
    }

    public static Integer displayScore(IpoStock stock) {
        if (stock.getAttractScore() != null) {
            return Math.round(stock.getAttractScore());
        }
        if (stock.getRecentGrowthScore() != null) {
            return stock.getRecentGrowthScore();
        }
        return 0;
    }

    public static LocalDate subscriptionStartDate(IpoStock stock) {
        if (stock.getSubscriptionStartDate() != null) {
            return stock.getSubscriptionStartDate();
        }
        return parseSubscriptionDateText(stock.getSubscriptionDate(), 0);
    }

    public static LocalDate subscriptionEndDate(IpoStock stock) {
        if (stock.getSubscriptionEndDate() != null) {
            return stock.getSubscriptionEndDate();
        }
        return parseSubscriptionDateText(stock.getSubscriptionDate(), 1);
    }

    public static LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static LocalDate parseSubscriptionDateText(String value, int index) {
        return parseSubscriptionDate(value, index);
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static LocalDate parseSubscriptionDate(String value, int index) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(value);
        Integer inferredYear = null;
        int found = 0;
        while (matcher.find()) {
            String year = matcher.group(1);
            String month;
            String day;
            if (year != null) {
                inferredYear = Integer.parseInt(year);
                month = matcher.group(2);
                day = matcher.group(3);
            } else {
                if (inferredYear == null) {
                    continue;
                }
                year = inferredYear.toString();
                month = matcher.group(4);
                day = matcher.group(5);
            }

            if (found == index) {
                try {
                    return LocalDate.parse(year + "." + month + "." + day, DATE_FORMATTER);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
            found++;
        }
        return null;
    }
}
