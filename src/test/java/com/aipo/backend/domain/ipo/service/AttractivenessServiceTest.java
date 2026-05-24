package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.ipo.dto.AttractivenessResponse;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttractivenessServiceTest {

    private final AttractivenessService attractivenessService = new AttractivenessService();

    @Test
    void parseNumber_handlesFormattedText() {
        assertThat(attractivenessService.parseNumber("876.72")).isEqualTo(876.72);
        assertThat(attractivenessService.parseNumber("847.09 :1")).isEqualTo(847.09);
        assertThat(attractivenessService.parseNumber("50.84%")).isEqualTo(50.84);
        assertThat(attractivenessService.parseNumber("1,234.56")).isEqualTo(1234.56);
        assertThat(attractivenessService.parseNumber("")).isNull();
        assertThat(attractivenessService.parseNumber("-")).isNull();
        assertThat(attractivenessService.parseNumber("|")).isNull();
        assertThat(attractivenessService.parseNumber(null)).isNull();
    }

    @Test
    void mapProfileType_usesRequestedProfileNames() {
        assertThat(attractivenessService.mapProfileType(InvestmentProfileType.AGGRESSIVE)).isEqualTo("aggressive");
        assertThat(attractivenessService.mapProfileType(InvestmentProfileType.NEUTRAL)).isEqualTo("balanced");
        assertThat(attractivenessService.mapProfileType(InvestmentProfileType.STABLE)).isEqualTo("conservative");
        assertThat(attractivenessService.mapProfileType(null)).isNull();
    }

    @Test
    void calculateForIpo_withoutProfile_selectsDefaultBalancedScore() {
        AttractivenessIpoProjection target = ipo(1L, "올릭스", "900", "30", "25", "60");
        List<AttractivenessIpoProjection> allIpos = List.of(
                ipo(1L, "올릭스", "900", "30", "25", "60"),
                ipo(2L, "비교기업", "100", "10", "55", "20"),
                ipo(3L, "비교스팩", "500", "20", "35", "40")
        );

        AttractivenessResponse response = attractivenessService.calculateForIpo(target, allIpos, null);

        assertThat(response.selectedProfile()).isNull();
        assertThat(response.selected()).isEqualTo(response.defaultScore());
        assertThat(response.balanced().score()).isEqualTo(response.defaultScore().score());
        assertThat(response.factorScores().subscriptionScore()).isNull();
        assertThat(response.factorScores().floatingStockScore()).isGreaterThan(50);
    }

    @Test
    void calculateForIpo_withStableProfile_selectsConservativeScoreAndSpacNotice() {
        AttractivenessIpoProjection target = ipo(1L, "AIPO스팩", "900", "30", "25", "60");
        List<AttractivenessIpoProjection> allIpos = List.of(target, ipo(2L, "비교기업", "100", "10", "55", "20"));

        AttractivenessResponse response = attractivenessService.calculateForIpo(
                target,
                allIpos,
                InvestmentProfileType.STABLE
        );

        assertThat(response.selectedProfile()).isEqualTo("conservative");
        assertThat(response.selected()).isEqualTo(response.conservative());
        assertThat(response.notice()).contains("스팩주");
    }

    @Test
    void calculateForIpo_withStableProfile_usesAbsoluteWeightedScores() {
        AttractivenessIpoProjection target = ipo(1L, "AIPO", "0", "0", "38.48", "61.52");

        AttractivenessResponse response = attractivenessService.calculateForIpo(
                target,
                List.of(target),
                InvestmentProfileType.STABLE
        );

        assertThat(response.factorScores().competitionScore()).isZero();
        assertThat(response.factorScores().instCommitmentScore()).isZero();
        assertThat(response.factorScores().floatingStockScore()).isEqualTo(62);
        assertThat(response.factorScores().lockupScore()).isEqualTo(62);
        assertThat(response.selectedProfile()).isEqualTo("conservative");
        assertThat(response.selected().score()).isEqualTo(29);
    }

    private AttractivenessIpoProjection ipo(
            Long stockId,
            String corpName,
            String competitionRatio,
            String instCommitmentRatio,
            String floatingStockRatio,
            String lockupTotalRatio
    ) {
        return new TestAttractivenessIpoProjection(
                stockId,
                corpName,
                competitionRatio,
                instCommitmentRatio,
                floatingStockRatio,
                lockupTotalRatio
        );
    }

    private record TestAttractivenessIpoProjection(
            Long stockId,
            String corpName,
            String competitionRatio,
            String instCommitmentRatio,
            String floatingStockRatio,
            String lockupTotalRatio
    ) implements AttractivenessIpoProjection {

        @Override
        public Long getStockId() {
            return stockId;
        }

        @Override
        public String getCorpName() {
            return corpName;
        }

        @Override
        public String getCompetitionRatio() {
            return competitionRatio;
        }

        @Override
        public String getInstCommitmentRatio() {
            return instCommitmentRatio;
        }

        @Override
        public String getFloatingStockRatio() {
            return floatingStockRatio;
        }

        @Override
        public String getLockupTotalRatio() {
            return lockupTotalRatio;
        }
    }
}
