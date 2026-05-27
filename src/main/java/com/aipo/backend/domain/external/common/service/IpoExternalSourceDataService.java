package com.aipo.backend.domain.external.common.service;

import com.aipo.backend.domain.external.common.dto.ExternalIpoSourceDataCommand;
import com.aipo.backend.domain.external.common.entity.IpoExternalSourceData;
import com.aipo.backend.domain.external.common.repository.IpoExternalSourceDataRepository;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IpoExternalSourceDataService {

    private final IpoExternalSourceDataRepository sourceDataRepository;
    private final IpoStockRepository ipoStockRepository;

    public IpoExternalSourceData upsert(ExternalIpoSourceDataCommand command) {
        IpoExternalSourceData sourceData = sourceDataRepository
                .findByProviderAndSourceTypeAndExternalKey(
                        command.provider(),
                        command.sourceType(),
                        command.externalKey()
                )
                .orElseGet(() -> sourceDataRepository.save(IpoExternalSourceData.create(
                        command.provider(),
                        command.sourceType(),
                        command.externalKey()
                )));

        sourceData.updateData(
                command.corpName(),
                command.dartCorpCode(),
                command.stockCode(),
                command.marketType(),
                command.subscriptionStartDate(),
                command.subscriptionEndDate(),
                command.demandForecastStartDate(),
                command.demandForecastEndDate(),
                command.refundDate(),
                command.listingDate(),
                command.confirmedOfferPrice(),
                command.leadManagers(),
                command.rawResponseId(),
                command.confidence()
        );
        return sourceData;
    }

    public boolean exists(String provider, String sourceType, String externalKey) {
        return sourceDataRepository
                .findByProviderAndSourceTypeAndExternalKey(provider, sourceType, externalKey)
                .isPresent();
    }

    public IpoStock mergeByDartCorpCode(String dartCorpCode) {
        List<IpoExternalSourceData> sources = sourceDataRepository.findAllByDartCorpCode(dartCorpCode);
        return merge(sources);
    }

    public IpoStock mergeByCorpName(String corpName) {
        List<IpoExternalSourceData> sources = sourceDataRepository.findAllByCorpName(corpName);
        return merge(sources);
    }

    private IpoStock merge(List<IpoExternalSourceData> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        List<IpoExternalSourceData> sortedSources = sources.stream()
                .sorted(Comparator.comparing(IpoExternalSourceData::getConfidence).reversed())
                .toList();
        List<IpoExternalSourceData> marketSources = sortedSources.stream()
                .filter(source -> source.getMarketType() != null)
                .filter(source -> !"OTHER".equalsIgnoreCase(source.getMarketType()))
                .toList();

        String dartCorpCode = firstNonBlank(sortedSources.stream().map(IpoExternalSourceData::getDartCorpCode).toList());
        String corpName = firstNonBlank(sortedSources.stream().map(IpoExternalSourceData::getCorpName).toList());
        String stockCode = firstNonBlank(sortedSources.stream().map(IpoExternalSourceData::getStockCode).toList());
        String marketType = firstNonBlank(
                (marketSources.isEmpty() ? sortedSources : marketSources)
                        .stream()
                        .map(IpoExternalSourceData::getMarketType)
                        .toList()
        );
        LocalDate subscriptionStartDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getSubscriptionStartDate).toList());
        LocalDate subscriptionEndDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getSubscriptionEndDate).toList());
        LocalDate demandForecastStartDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getDemandForecastStartDate).toList());
        LocalDate demandForecastEndDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getDemandForecastEndDate).toList());
        LocalDate refundDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getRefundDate).toList());
        LocalDate listingDate = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getListingDate).toList());
        BigDecimal confirmedOfferPrice = firstNonNull(sortedSources.stream().map(IpoExternalSourceData::getConfirmedOfferPrice).toList());
        String leadManagers = firstNonBlank(sortedSources.stream().map(IpoExternalSourceData::getLeadManagers).toList());

        IpoStock stock = findOrCreateStock(
                dartCorpCode,
                corpName,
                stockCode,
                marketType,
                confirmedOfferPrice,
                subscriptionStartDate,
                subscriptionEndDate
        );
        if (stock == null) {
            return null;
        }
        stock.supplementFromKind(stockCode, marketType, listingDate);
        stock.updateSupplementalDates(demandForecastStartDate, demandForecastEndDate, refundDate, listingDate);
        stock.updateUnderwriter(leadManagers);

        return stock;
    }

    private IpoStock findOrCreateStock(
            String dartCorpCode,
            String corpName,
            String stockCode,
            String marketType,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate
    ) {
        if (corpName == null || corpName.isBlank()) {
            return null;
        }

        List<IpoStock> existingStocks = dartCorpCode == null || dartCorpCode.isBlank()
                ? List.of()
                : ipoStockRepository.findAllByDartCorpCode(dartCorpCode);

        if (!existingStocks.isEmpty()) {
            IpoStock existing = existingStocks.get(0);
                    existing.updateFromExternal(
                            corpName,
                            corpName,
                            stockCode,
                            marketType,
                            confirmedOfferPrice,
                            subscriptionStartDate,
                            subscriptionEndDate
                    );
                    return existing;
        }

        return ipoStockRepository.save(IpoStock.createFromExternal(
                corpName,
                corpName,
                stockCode,
                dartCorpCode,
                marketType,
                confirmedOfferPrice,
                subscriptionStartDate,
                subscriptionEndDate
        ));
    }

    private String firstNonBlank(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private <T> T firstNonNull(List<T> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
