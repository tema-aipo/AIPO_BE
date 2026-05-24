package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private IpoStockRepository ipoStockRepository;

    @Mock
    private UserFavoriteStockRepository userFavoriteStockRepository;

    @Mock
    private IpoLeadManagerRepository ipoLeadManagerRepository;

    @Mock
    private IpoScheduleRepository ipoScheduleRepository;

    @Mock
    private UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    @Spy
    private AttractivenessService attractivenessService = new AttractivenessService();

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    @DisplayName("현재 로그인 사용자의 관심종목 목록을 응답 DTO로 변환한다")
    void getFavorites_returnsFavoriteStockResponses() {
        Long userId = 10L;
        IpoStock firstStock = ipoStock(
                1L,
                "AIPO",
                "공시문서 분석 기업",
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29)
        );
        IpoStock secondStock = ipoStock(
                2L,
                "BIPO",
                "데이터 플랫폼 기업",
                new BigDecimal("18000.00"),
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4)
        );

        when(userFavoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                favorite(userId, firstStock, LocalDateTime.of(2026, 4, 21, 10, 0)),
                favorite(userId, secondStock, LocalDateTime.of(2026, 4, 20, 10, 0))
        ));

        when(ipoLeadManagerRepository.findAllByStock_IdIn(any())).thenReturn(List.of());
        when(ipoScheduleRepository.findAllByStock_IdInAndScheduleType(any(), any())).thenReturn(List.of());

        List<FavoriteStockResponse> responses = favoriteService.getFavorites(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).ipoId()).isEqualTo(1L);
        assertThat(responses.get(0).companyName()).isEqualTo("AIPO");
        assertThat(responses.get(0).oneLineDescription()).isEqualTo("공시문서 분석 기업");
        assertThat(responses.get(0).confirmedOfferPrice()).isEqualByComparingTo("15000.00");
        assertThat(responses.get(0).subscriptionPeriod().startDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(responses.get(0).subscriptionPeriod().endDate()).isEqualTo(LocalDate.of(2026, 4, 29));
        assertThat(responses.get(0).isFavorite()).isTrue();
        assertThat(responses.get(0).leadManager()).isEqualTo("-");
        assertThat(responses.get(0).status()).isNotNull();
        assertThat(responses.get(0).dateRange()).isNotNull();
        assertThat(responses.get(1).ipoId()).isEqualTo(2L);
        verify(userFavoriteStockRepository).findAllByUserIdOrderByCreatedAtDesc(userId);
        verify(ipoLeadManagerRepository).findAllByStock_IdIn(any());
        verify(ipoScheduleRepository).findAllByStock_IdInAndScheduleType(any(), any());
    }

    @Test
    @DisplayName("관심종목이 없으면 빈 리스트를 반환한다")
    void getFavorites_whenNoFavorites_returnsEmptyList() {
        Long userId = 10L;

        when(userFavoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        List<FavoriteStockResponse> responses = favoriteService.getFavorites(userId);

        assertThat(responses).isEmpty();
        verify(userFavoriteStockRepository).findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("존재하는 공모주를 관심종목으로 등록한다")
    void addFavorite_success() {
        Long userId = 10L;
        Long ipoId = 1L;
        IpoStock stock = ipoStock(
                ipoId,
                "AIPO",
                "공시문서 분석 기업",
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29)
        );

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.of(stock));
        when(userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipoId)).thenReturn(false);

        favoriteService.addFavorite(userId, ipoId);

        verify(ipoStockRepository).findById(ipoId);
        verify(userFavoriteStockRepository).existsByUserIdAndStock_Id(userId, ipoId);
        verify(userFavoriteStockRepository).save(any(UserFavoriteStock.class));
    }

    @Test
    @DisplayName("없는 공모주를 등록하면 IPO_NOT_FOUND 예외가 발생한다")
    void addFavorite_whenIpoDoesNotExist_throwIpoNotFound() {
        Long userId = 10L;
        Long ipoId = 999L;

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(userId, ipoId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IPO_NOT_FOUND);

        verify(userFavoriteStockRepository, never()).save(any(UserFavoriteStock.class));
    }

    @Test
    @DisplayName("중복 관심종목 등록 시 DUPLICATE_FAVORITE_STOCK 예외가 발생한다")
    void addFavorite_whenDuplicate_throwDuplicateFavoriteStock() {
        Long userId = 10L;
        Long ipoId = 1L;
        IpoStock stock = ipoStock(
                ipoId,
                "AIPO",
                "공시문서 분석 기업",
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29)
        );

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.of(stock));
        when(userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipoId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(userId, ipoId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_FAVORITE_STOCK);

        verify(userFavoriteStockRepository, never()).save(any(UserFavoriteStock.class));
    }

    @Test
    @DisplayName("등록된 관심종목을 삭제한다")
    void removeFavorite_success() {
        Long userId = 10L;
        Long ipoId = 1L;
        UserFavoriteStock favoriteStock = favorite(
                userId,
                ipoStock(
                        ipoId,
                        "AIPO",
                        "공시문서 분석 기업",
                        new BigDecimal("15000.00"),
                        LocalDate.of(2026, 4, 28),
                        LocalDate.of(2026, 4, 29)
                ),
                LocalDateTime.of(2026, 4, 21, 10, 0)
        );

        when(userFavoriteStockRepository.findByUserIdAndStock_Id(userId, ipoId)).thenReturn(Optional.of(favoriteStock));

        favoriteService.removeFavorite(userId, ipoId);

        verify(userFavoriteStockRepository).findByUserIdAndStock_Id(userId, ipoId);
        verify(userFavoriteStockRepository).delete(favoriteStock);
    }

    @Test
    @DisplayName("없는 관심종목 삭제 시 FAVORITE_STOCK_NOT_FOUND 예외가 발생한다")
    void removeFavorite_whenFavoriteDoesNotExist_throwFavoriteStockNotFound() {
        Long userId = 10L;
        Long ipoId = 1L;

        when(userFavoriteStockRepository.findByUserIdAndStock_Id(userId, ipoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(userId, ipoId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FAVORITE_STOCK_NOT_FOUND);

        verify(userFavoriteStockRepository, never()).delete(any(UserFavoriteStock.class));
    }

    private UserFavoriteStock favorite(Long userId, IpoStock stock, LocalDateTime createdAt) {
        UserFavoriteStock favoriteStock = instantiate(UserFavoriteStock.class);
        ReflectionTestUtils.setField(favoriteStock, "userId", userId);
        ReflectionTestUtils.setField(favoriteStock, "stock", stock);
        ReflectionTestUtils.setField(favoriteStock, "createdAt", createdAt);
        return favoriteStock;
    }

    private IpoStock ipoStock(
            Long id,
            String companyName,
            String oneLineDescription,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate
    ) {
        IpoStock ipoStock = instantiate(IpoStock.class);
        ReflectionTestUtils.setField(ipoStock, "id", id);
        ReflectionTestUtils.setField(ipoStock, "companyName", companyName);
        ReflectionTestUtils.setField(ipoStock, "oneLineDescription", oneLineDescription);
        ReflectionTestUtils.setField(ipoStock, "confirmedOfferPrice", confirmedOfferPrice);
        ReflectionTestUtils.setField(ipoStock, "subscriptionStartDate", subscriptionStartDate);
        ReflectionTestUtils.setField(ipoStock, "subscriptionEndDate", subscriptionEndDate);
        ReflectionTestUtils.setField(ipoStock, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ipoStock, "updatedAt", LocalDateTime.now());
        return ipoStock;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
