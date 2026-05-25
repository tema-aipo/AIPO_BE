package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.ipo.entity.IpoStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IpoStockViewMapper {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})|(?<!\\d)(\\d{1,2})[.\\-/](\\d{1,2})(?!\\d)");

    private IpoStockViewMapper() {
    }

    public static String displayName(IpoStock stock) {
        return displayName(stock.getCorpName(), stock.getStockCode(), stock.getCorpName());
    }

    public static String displayName(String companyName, String stockName, String corpName) {
        return firstText(corpName, companyName, stockName, "");
    }

    public static String displayStockName(IpoStock stock) {
        return firstText(stock.getCorpName(), stock.getStockCode(), "");
    }

    public static String displayCompanyName(IpoStock stock) {
        return firstText(stock.getCorpName(), stock.getStockCode(), "");
    }

    public static String displayCompanyName(String companyName, String stockName, String corpName) {
        return firstText(corpName, companyName, stockName, "");
    }

    public static BigDecimal offerPrice(IpoStock stock) {
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
        return parseSubscriptionDateText(stock.getSubscriptionDate(), 0);
    }

    public static LocalDate subscriptionEndDate(IpoStock stock) {
        LocalDate endDate = parseSubscriptionDateText(stock.getSubscriptionDate(), 1);
        return endDate != null ? endDate : parseSubscriptionDateText(stock.getSubscriptionDate(), 0);
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
        return parseDateText(value, index);
    }

    public static LocalDate parseDateText(String value, int index) {
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
                    return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            found++;
        }
        return null;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

}
