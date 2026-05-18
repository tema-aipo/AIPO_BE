package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.ipo.entity.*;
import com.aipo.backend.domain.ipo.repository.*;
import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final IpoStockRepository ipoStockRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;
    private final IpoScheduleRepository ipoScheduleRepository; // 추가

    public List<FavoriteStockResponse> getFavorites(Long userId) {
        List<UserFavoriteStock> favorites = userFavoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> stockIds = favorites.stream()
                .map(f -> f.getStock().getId())
                .toList();

        // 1. Batch load lead managers
        List<IpoLeadManager> managers = ipoLeadManagerRepository.findAllByStock_IdIn(stockIds);
        Map<Long, List<IpoLeadManager>> managersByStockId = managers.stream()
                .collect(Collectors.groupingBy(m -> m.getStock().getId()));

        // 2. Batch load refund schedules
        List<IpoSchedule> refundSchedules = ipoScheduleRepository
                .findAllByStock_IdInAndScheduleType(stockIds, ScheduleType.REFUND);
        Map<Long, java.time.LocalDate> refundDateByStockId = refundSchedules.stream()
                .collect(Collectors.toMap(
                        s -> s.getStock().getId(),
                        IpoSchedule::getScheduleDate,
                        (a, b) -> a
                ));

        return favorites.stream()
                .map(favoriteStock -> toResponse(favoriteStock, managersByStockId, refundDateByStockId))
                .toList();
    }

    private FavoriteStockResponse toResponse(
            UserFavoriteStock favoriteStock,
            Map<Long, List<IpoLeadManager>> managersByStockId,
            Map<Long, java.time.LocalDate> refundDateByStockId
    ) {
        IpoStock stock = favoriteStock.getStock();
        Long stockId = stock.getId();

        // 1. Attraction Score
        Integer score = stock.getAttractScore() == null ? null : Math.round(stock.getAttractScore());

        // 2. Lead Manager
        List<IpoLeadManager> stockManagers = managersByStockId.getOrDefault(stockId, Collections.emptyList());
        String leadManager = stockManagers.isEmpty() ? "-" : stockManagers.get(0).getManagerName();

        // 3. Status and DateRange
        String status = null;
        String dateRange = "-";

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate subStart = stock.getSubscriptionStartDate();
        java.time.LocalDate subEnd = stock.getSubscriptionEndDate();
        java.time.LocalDate listDate = stock.getListingDate();
        java.time.LocalDate refundDate = refundDateByStockId.get(stockId);

        if (listDate != null && (today.isEqual(listDate) || today.isAfter(listDate))) {
            status = "상장";
            dateRange = String.format("%02d.%02d", listDate.getMonthValue(), listDate.getDayOfMonth());
        } else if (subStart != null && subEnd != null && (today.isEqual(subStart) || today.isEqual(subEnd) || (today.isAfter(subStart) && today.isBefore(subEnd)))) {
            status = "청약";
            dateRange = String.format("%02d.%02d ~ %02d.%02d",
                    subStart.getMonthValue(), subStart.getDayOfMonth(),
                    subEnd.getMonthValue(), subEnd.getDayOfMonth());
        } else if (subStart != null && today.isBefore(subStart)) {
            status = "수요예측";
            if (subEnd != null) {
                dateRange = String.format("%02d.%02d ~ %02d.%02d",
                        subStart.getMonthValue(), subStart.getDayOfMonth(),
                        subEnd.getMonthValue(), subEnd.getDayOfMonth());
            } else {
                dateRange = String.format("%02d.%02d", subStart.getMonthValue(), subStart.getDayOfMonth());
            }
        } else if (refundDate != null && today.isEqual(refundDate)) {
            status = "환불";
            dateRange = String.format("%02d.%02d", refundDate.getMonthValue(), refundDate.getDayOfMonth());
        } else if (subStart != null && subEnd != null && today.isAfter(subEnd)) {
            status = "청약종료";
            dateRange = String.format("%02d.%02d ~ %02d.%02d",
                    subStart.getMonthValue(), subStart.getDayOfMonth(),
                    subEnd.getMonthValue(), subEnd.getDayOfMonth());
        }

        return new FavoriteStockResponse(
                stock.getId(),
                stock.getCompanyName(),
                stock.getOneLineDescription(),
                stock.getConfirmedOfferPrice(),
                new DateRange(stock.getSubscriptionStartDate(), stock.getSubscriptionEndDate()),
                true,
                score,
                leadManager,
                status,
                dateRange
        );
    }

    @Transactional
    public void addFavorite(Long userId, Long ipoId) {
        IpoStock stock = ipoStockRepository.findById(ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.IPO_NOT_FOUND));

        if (userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipoId)) {
            throw new CustomException(ErrorCode.DUPLICATE_FAVORITE_STOCK);
        }

        userFavoriteStockRepository.save(UserFavoriteStock.create(userId, stock));
    }

    @Transactional
    public void removeFavorite(Long userId, Long ipoId) {
        UserFavoriteStock favoriteStock = userFavoriteStockRepository.findByUserIdAndStock_Id(userId, ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_STOCK_NOT_FOUND));

        userFavoriteStockRepository.delete(favoriteStock);
    }
}
