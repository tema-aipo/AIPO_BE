package com.aipo.backend.domain.external.common.service;

import com.aipo.backend.domain.external.common.dto.ExternalIpoSourceDataCommand;
import com.aipo.backend.domain.external.common.entity.IpoExternalSourceData;
import com.aipo.backend.domain.external.common.repository.IpoExternalSourceDataRepository;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IpoExternalSourceDataService {

    private final IpoExternalSourceDataRepository sourceDataRepository;
    private final IpoStockRepository ipoStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;
    private final IpoScheduleRepository ipoScheduleRepository;

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

        upsertSchedule(stock, ScheduleType.SUBSCRIPTION_START, subscriptionStartDate, "MERGED subscription start");
        upsertSchedule(stock, ScheduleType.SUBSCRIPTION_END, subscriptionEndDate, "MERGED subscription end");
        upsertSchedule(stock, ScheduleType.DEMAND_FORECAST_START, demandForecastStartDate, "MERGED demand forecast start");
        upsertSchedule(stock, ScheduleType.DEMAND_FORECAST_END, demandForecastEndDate, "MERGED demand forecast end");
        upsertSchedule(stock, ScheduleType.REFUND, refundDate, "MERGED refund");
        upsertSchedule(stock, ScheduleType.LISTING, listingDate, "MERGED listing");
        replaceLeadManagers(stock, leadManagers);

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

    private void upsertSchedule(IpoStock stock, ScheduleType scheduleType, LocalDate date, String note) {
        if (stock == null || date == null) {
            return;
        }

        ipoScheduleRepository.findByStock_IdAndScheduleType(stock.getId(), scheduleType)
                .ifPresentOrElse(
                        schedule -> schedule.updateDate(date, note),
                        () -> ipoScheduleRepository.save(IpoSchedule.create(stock, scheduleType, date, note))
                );
    }

    private void replaceLeadManagers(IpoStock stock, String leadManagers) {
        if (stock == null || leadManagers == null || leadManagers.isBlank()) {
            return;
        }

        List<String> managerNames = Arrays.stream(leadManagers.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (managerNames.isEmpty()) {
            return;
        }

        ipoLeadManagerRepository.deleteAllByStock_Id(stock.getId());
        ipoLeadManagerRepository.flush();
        for (int i = 0; i < managerNames.size(); i++) {
            ipoLeadManagerRepository.save(IpoLeadManager.create(stock, managerNames.get(i), i + 1));
        }
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
