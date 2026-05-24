package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.ipo.dto.AttractivenessResponse;
import com.aipo.backend.domain.ipo.dto.FactorScoresResponse;
import com.aipo.backend.domain.ipo.dto.ProfileAttractivenessScore;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AttractivenessService {

    private static final String PROFILE_AGGRESSIVE = "aggressive";
    private static final String PROFILE_BALANCED = "balanced";
    private static final String PROFILE_CONSERVATIVE = "conservative";
    private static final double AGGRESSIVE_REMAINING_WEIGHT = 0.70;
    private static final double BALANCED_REMAINING_WEIGHT = 0.85;
    private static final double CONSERVATIVE_REMAINING_WEIGHT = 0.95;
    private static final double COMPETITION_RATIO_SCORE_CAP = 1000.0;

    private static final String DEFAULT_REASON = "기관 수요예측 경쟁률, 의무보유확약, 유통가능물량, 보호예수 비율을 종합하여 산출한 기본 매력지수입니다.";
    private static final String AGGRESSIVE_REASON = "기관 수요예측 경쟁률을 중심으로 단기 수급 기대를 반영한 결과입니다.";
    private static final String BALANCED_REASON = "기관 수요와 수급 안정성을 균형 있게 반영한 결과입니다.";
    private static final String CONSERVATIVE_REASON = "의무보유확약 비율, 유통가능물량 비율, 보호예수 비율을 중심으로 안정성을 평가한 결과입니다.";
    private static final String SPAC_NOTICE = "이 종목은 스팩주로, 일반 기업 IPO와 구조가 다를 수 있습니다. 의무보유확약 등 일부 지표는 일반 IPO와 해석이 다를 수 있으므로 참고용으로 확인하세요.";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

    public AttractivenessResponse calculateForIpo(
            AttractivenessIpoProjection targetIpo,
            List<AttractivenessIpoProjection> allIpos,
            InvestmentProfileType currentProfileType
    ) {
        FactorScoresResponse factorScores = calculateFactorScores(targetIpo, allIpos);

        int aggressiveScore = calculateWeightedScore(
                factorScores,
                0.35 / AGGRESSIVE_REMAINING_WEIGHT,
                0.15 / AGGRESSIVE_REMAINING_WEIGHT,
                0.15 / AGGRESSIVE_REMAINING_WEIGHT,
                0.05 / AGGRESSIVE_REMAINING_WEIGHT
        );
        int balancedScore = calculateWeightedScore(
                factorScores,
                0.30 / BALANCED_REMAINING_WEIGHT,
                0.25 / BALANCED_REMAINING_WEIGHT,
                0.25 / BALANCED_REMAINING_WEIGHT,
                0.05 / BALANCED_REMAINING_WEIGHT
        );
        int conservativeScore = calculateWeightedScore(
                factorScores,
                0.15 / CONSERVATIVE_REMAINING_WEIGHT,
                0.35 / CONSERVATIVE_REMAINING_WEIGHT,
                0.30 / CONSERVATIVE_REMAINING_WEIGHT,
                0.15 / CONSERVATIVE_REMAINING_WEIGHT
        );

        ProfileAttractivenessScore defaultScore =
                new ProfileAttractivenessScore(balancedScore, getGrade(balancedScore), DEFAULT_REASON);
        ProfileAttractivenessScore aggressive =
                new ProfileAttractivenessScore(aggressiveScore, getGrade(aggressiveScore), AGGRESSIVE_REASON);
        ProfileAttractivenessScore balanced =
                new ProfileAttractivenessScore(balancedScore, getGrade(balancedScore), BALANCED_REASON);
        ProfileAttractivenessScore conservative =
                new ProfileAttractivenessScore(conservativeScore, getGrade(conservativeScore), CONSERVATIVE_REASON);

        String selectedProfile = mapProfileType(currentProfileType);
        ProfileAttractivenessScore selected = switch (selectedProfile == null ? "" : selectedProfile) {
            case PROFILE_AGGRESSIVE -> aggressive;
            case PROFILE_BALANCED -> balanced;
            case PROFILE_CONSERVATIVE -> conservative;
            default -> defaultScore;
        };

        return new AttractivenessResponse(
                defaultScore,
                selectedProfile,
                selected,
                aggressive,
                balanced,
                conservative,
                factorScores,
                detectSpacNotice(targetIpo.getCorpName())
        );
    }

    public FactorScoresResponse calculateFactorScores(
            AttractivenessIpoProjection targetIpo,
            List<AttractivenessIpoProjection> allIpos
    ) {
        return new FactorScoresResponse(
                normalizeCompetitionRatio(parseNumber(targetIpo.getCompetitionRatio())),
                null,
                normalizePercent(parseNumber(targetIpo.getInstCommitmentRatio())),
                invertPercent(parseNumber(targetIpo.getFloatingStockRatio())),
                normalizePercent(parseNumber(targetIpo.getLockupTotalRatio()))
        );
    }

    public int calculatePercentileScore(Double targetValue, List<Double> values, boolean lowerIsBetter) {
        int score = normalizePercent(targetValue);
        return lowerIsBetter ? 100 - score : score;
    }

    public Double parseNumber(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replace(",", "");
        if (normalized.isEmpty()
                || "-".equals(normalized)
                || "|".equals(normalized)) {
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public String getGrade(int score) {
        if (score >= 85) {
            return "매우 높음";
        }
        if (score >= 70) {
            return "높음";
        }
        if (score >= 55) {
            return "보통";
        }
        if (score >= 40) {
            return "낮음";
        }
        return "주의";
    }

    public String mapProfileType(InvestmentProfileType profileType) {
        if (profileType == null) {
            return null;
        }

        return switch (profileType) {
            case AGGRESSIVE -> PROFILE_AGGRESSIVE;
            case NEUTRAL -> PROFILE_BALANCED;
            case STABLE -> PROFILE_CONSERVATIVE;
        };
    }

    public String detectSpacNotice(String corpName) {
        if (corpName == null || corpName.isBlank()) {
            return null;
        }

        String upperName = corpName.toUpperCase(Locale.ROOT);
        if (corpName.contains("스팩")
                || upperName.contains("SPAC")
                || corpName.contains("기업인수목적")) {
            return SPAC_NOTICE;
        }
        return null;
    }

    private int calculateWeightedScore(
            FactorScoresResponse factorScores,
            double competitionWeight,
            double instCommitmentWeight,
            double floatingStockWeight,
            double lockupWeight
    ) {
        return (int) Math.round(
                factorScores.competitionScore() * competitionWeight
                        + factorScores.instCommitmentScore() * instCommitmentWeight
                        + factorScores.floatingStockScore() * floatingStockWeight
                        + factorScores.lockupScore() * lockupWeight
        );
    }

    private int normalizeCompetitionRatio(Double value) {
        if (value == null) {
            return 0;
        }
        return clampScore((value / COMPETITION_RATIO_SCORE_CAP) * 100);
    }

    private int normalizePercent(Double value) {
        if (value == null) {
            return 0;
        }
        return clampScore(value);
    }

    private int invertPercent(Double value) {
        if (value == null) {
            return 0;
        }
        return 100 - normalizePercent(value);
    }

    private int clampScore(double value) {
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }
}
