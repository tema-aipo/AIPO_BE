package com.aipo.backend.domain.home.service;

import com.aipo.backend.domain.home.dto.AttractivenessItem;
import com.aipo.backend.domain.home.dto.HomeResponse;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private IpoStockRepository ipoStockRepository;

    @Mock
    private UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    @Mock
    private IpoLeadManagerRepository ipoLeadManagerRepository;

    private final AttractivenessService attractivenessService = new AttractivenessService();

    @Test
    void getHome_subscriptionUpcoming_returnsActiveAndUpcomingOnlyInDateOrder() {
        HomeService homeService = new HomeService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService,
                ipoLeadManagerRepository
        );
        LocalDate today = LocalDate.now();
        AttractivenessItem past = item(1L, "과거청약", today.minusDays(10), today.minusDays(8));
        AttractivenessItem futureLater = item(2L, "미래청약2", today.plusDays(5), today.plusDays(6));
        AttractivenessItem active = item(3L, "진행중청약", today.minusDays(1), today.plusDays(1));
        AttractivenessItem futureSoon = item(4L, "미래청약1", today.plusDays(2), today.plusDays(3));

        when(ipoStockRepository.findFeaturedIpos()).thenReturn(List.of());
        when(ipoStockRepository.findTrendingIpos()).thenReturn(List.of());
        when(ipoStockRepository.findAttractivenessBySubscriptionUpcoming())
                .thenReturn(List.of(past, futureLater, active, futureSoon));
        when(ipoLeadManagerRepository.findAllByStock_IdIn(List.of(3L, 4L, 2L))).thenReturn(List.of());
        when(ipoStockRepository.findUnderwritersByStockIds(List.of(3L, 4L, 2L))).thenReturn(Map.of());

        HomeResponse response = homeService.getHome("subscriptionUpcoming", null);

        assertThat(response.attractiveness().selectedTab()).isEqualTo("subscriptionUpcoming");
        assertThat(response.attractiveness().items())
                .extracting(AttractivenessItem::ipoId)
                .containsExactly(3L, 4L, 2L);
    }

    private AttractivenessItem item(Long ipoId, String name, LocalDate startDate, LocalDate endDate) {
        return new AttractivenessItem(
                ipoId,
                name,
                0,
                startDate,
                endDate,
                null,
                null,
                null,
                null
        );
    }
}
