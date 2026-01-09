package com.transportoptimizer.Services;

import com.transportoptimizer.entity.ProviderFare;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PriceNormalizationServices{

    public ProviderFare normalize(ProviderFare rawFare, double distanceKm) {
        if (rawFare == null) {
            return null;
        }

        double normalizedPrice = roundTo2(rawFare.getPrice());

        Map<String, Object> meta = rawFare.getMetadata() != null
                ? new HashMap<>(rawFare.getMetadata())
                : new HashMap<>();

        meta.put("normalized", true);
        meta.put("distanceKm", distanceKm);

        ProviderFare normalized = ProviderFare.builder()
                .providerId(rawFare.getProviderId())
                .providerName(rawFare.getProviderName())
                .price(normalizedPrice)
                .distanceKm(distanceKm)
                .etaMinutes(rawFare.getEtaMinutes())
                .isSurge(rawFare.isSurge())
                .currency("INR")
                .metadata(meta)
                .vehicleType(rawFare.getVehicleType())
                .build();

        log.debug("Normalized fare for provider {}: {} (INR) at distance {} km",
                rawFare.getProviderId(), normalizedPrice, distanceKm);

        return normalized;
    }

    public List<ProviderFare> normalizeAll(List<ProviderFare> raw, double distanceKm) {
        if (raw == null) {
            return List.of();
        }
        List<ProviderFare> normalized = new ArrayList<>();
        for (ProviderFare fare : raw) {
            normalized.add(normalize(fare, distanceKm));
        }
        return normalized;
    }

    private int roundTo2(double value) {
        return (int)value ;
    }
}
