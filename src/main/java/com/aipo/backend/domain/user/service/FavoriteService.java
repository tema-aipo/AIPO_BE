package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final IpoStockRepository ipoStockRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;

    public List<FavoriteStockResponse> getFavorites(Long userId) {
        return userFavoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FavoriteStockResponse toResponse(UserFavoriteStock favoriteStock) {
        IpoStock stock = favoriteStock.getStock();

        return new FavoriteStockResponse(
                stock.getId(),
                stock.getCompanyName(),
                stock.getOneLineDescription(),
                stock.getConfirmedOfferPrice(),
                new DateRange(stock.getSubscriptionStartDate(), stock.getSubscriptionEndDate()),
                true
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
