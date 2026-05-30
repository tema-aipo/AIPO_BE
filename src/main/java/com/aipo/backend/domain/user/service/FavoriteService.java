package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import com.aipo.backend.domain.ipo.service.IpoStockViewMapper;
import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final IpoStockRepository ipoStockRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;

    public List<FavoriteStockResponse> getFavorites(Long userId) {
        List<UserFavoriteStock> favorites = userFavoriteStockRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> stockIds = favorites.stream()
                .map(f -> f.getStock().getId())
                .toList();

        Map<Long, String> underwritersByStockId = ipoStockRepository.findUnderwritersByStockIds(stockIds);
        Map<Long, Integer> scoreByStockId = calculateFavoriteScores(
                favorites,
                currentProfileType(userId)
        );

        return favorites.stream()
                .map(favoriteStock -> toResponse(
                        favoriteStock,
                        underwritersByStockId,
                        scoreByStockId
                ))
                .toList();
    }

    private FavoriteStockResponse toResponse(
            UserFavoriteStock favoriteStock,
            Map<Long, String> underwritersByStockId,
            Map<Long, Integer> scoreByStockId
    ) {
        IpoStock stock = favoriteStock.getStock();
        Long stockId = stock.getId();

        // 1. Attraction Score
        Integer fallbackScore = stock.getAttractScore() == null ? null : Math.round(stock.getAttractScore());
        Integer score = scoreByStockId.getOrDefault(stockId, fallbackScore);

        String leadManager = firstUnderwriter(underwritersByStockId.get(stockId));

        // 3. Status and DateRange
        String status = null;
        String dateRange = "-";

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate subStart = stock.getSubscriptionStartDate();
        java.time.LocalDate subEnd = stock.getSubscriptionEndDate();
        java.time.LocalDate listDate = stock.getListingDate();
        java.time.LocalDate refundDate = IpoStockViewMapper.parseDateText(stock.getRefundDate(), 0);

        if (listDate != null && (today.isEqual(listDate) || today.isAfter(listDate))) {
            status = "\uC0C1\uC7A5";
            dateRange = String.format("%02d.%02d", listDate.getMonthValue(), listDate.getDayOfMonth());
        } else if (subStart != null && subEnd != null && (today.isEqual(subStart) || today.isEqual(subEnd) || (today.isAfter(subStart) && today.isBefore(subEnd)))) {
            status = "\uCCAD\uC57D";
            dateRange = String.format("%02d.%02d ~ %02d.%02d",
                    subStart.getMonthValue(), subStart.getDayOfMonth(),
                    subEnd.getMonthValue(), subEnd.getDayOfMonth());
        } else if (subStart != null && today.isBefore(subStart)) {
            status = "\uC218\uC694\uC608\uCE21";
            if (subEnd != null) {
                dateRange = String.format("%02d.%02d ~ %02d.%02d",
                        subStart.getMonthValue(), subStart.getDayOfMonth(),
                        subEnd.getMonthValue(), subEnd.getDayOfMonth());
            } else {
                dateRange = String.format("%02d.%02d", subStart.getMonthValue(), subStart.getDayOfMonth());
            }
        } else if (refundDate != null && today.isEqual(refundDate)) {
            status = "\uD658\uBD88";
            dateRange = String.format("%02d.%02d", refundDate.getMonthValue(), refundDate.getDayOfMonth());
        } else if (subStart != null && subEnd != null && today.isAfter(subEnd)) {
            status = "\uCCAD\uC57D\uC885\uB8CC";
            dateRange = String.format("%02d.%02d ~ %02d.%02d",
                    subStart.getMonthValue(), subStart.getDayOfMonth(),
                    subEnd.getMonthValue(), subEnd.getDayOfMonth());
        }

        return new FavoriteStockResponse(
                stock.getId(),
                IpoStockViewMapper.displayCompanyName(stock),
                stock.getOneLineDescription(),
                stock.getConfirmedOfferPrice(),
                new DateRange(stock.getSubscriptionStartDate(), stock.getSubscriptionEndDate()),
                true,
                score,
                leadManager,
                status,
                dateRange,
                favoriteStock.isNotificationEnabled()
        );
    }

    private InvestmentProfileType currentProfileType(Long userId) {
        return userInvestmentProfileResultRepository
                .findTopByUserIdAndCurrentTrueOrderByCreatedAtDescIdDesc(userId)
                .map(result -> result.getProfileType())
                .orElse(null);
    }

    private Map<Long, Integer> calculateFavoriteScores(
            List<UserFavoriteStock> favorites,
            InvestmentProfileType currentProfileType
    ) {
        List<AttractivenessIpoProjection> allIpos;
        try {
            allIpos = ipoStockRepository.findAllForAttractiveness();
        } catch (DataAccessException exception) {
            allIpos = favorites.stream()
                    .map(favorite -> fallbackAttractivenessProjection(favorite.getStock()))
                    .toList();
        }

        if (allIpos == null || allIpos.isEmpty()) {
            allIpos = favorites.stream()
                    .map(favorite -> fallbackAttractivenessProjection(favorite.getStock()))
                    .toList();
        }

        Map<Long, AttractivenessIpoProjection> projectionByStockId = allIpos.stream()
                .collect(Collectors.toMap(
                        AttractivenessIpoProjection::getStockId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        List<AttractivenessIpoProjection> finalAllIpos = allIpos;

        return favorites.stream()
                .collect(Collectors.toMap(
                        favorite -> favorite.getStock().getId(),
                        favorite -> {
                            IpoStock stock = favorite.getStock();
                            AttractivenessIpoProjection target = projectionByStockId.getOrDefault(
                                    stock.getId(),
                                    fallbackAttractivenessProjection(stock)
                            );
                            return attractivenessService
                                    .calculateForIpo(target, finalAllIpos, currentProfileType)
                                    .selected()
                                    .score();
                        },
                        (existing, replacement) -> existing
                ));
    }

    private AttractivenessIpoProjection fallbackAttractivenessProjection(IpoStock stock) {
        return new SimpleAttractivenessIpoProjection(
                stock.getId(),
                IpoStockViewMapper.displayCompanyName(stock),
                null,
                null,
                null,
                null
        );
    }

    private String firstUnderwriter(String underwriter) {
        if (underwriter == null || underwriter.isBlank()) {
            return "-";
        }

        for (String name : underwriter.split(",")) {
            String trimmed = name.trim();
            if (hasTextValue(trimmed)) {
                return trimmed;
            }
        }
        return "-";
    }

    private boolean hasTextValue(String value) {
        return value != null && !value.isBlank() && !"-".equals(value.trim());
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

    @Transactional
    public FavoriteStockResponse updateFavoriteNotification(Long userId, Long ipoId, boolean enabled) {
        UserFavoriteStock favoriteStock = userFavoriteStockRepository.findByUserIdAndStock_Id(userId, ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_STOCK_NOT_FOUND));

        favoriteStock.updateNotificationEnabled(enabled);

        Map<Long, String> underwritersByStockId = ipoStockRepository.findUnderwritersByStockIds(List.of(ipoId));
        Map<Long, Integer> scoreByStockId = calculateFavoriteScores(
                List.of(favoriteStock),
                currentProfileType(userId)
        );
        return toResponse(favoriteStock, underwritersByStockId, scoreByStockId);
    }

    private record SimpleAttractivenessIpoProjection(
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
