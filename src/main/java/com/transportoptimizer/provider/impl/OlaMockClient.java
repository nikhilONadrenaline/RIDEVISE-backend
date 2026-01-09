package com.transportoptimizer.provider.impl;

import com.transportoptimizer.entity.ProviderFare;
import com.transportoptimizer.provider.ProviderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OlaMockClient implements ProviderClient {

    // Base fare per vehicle type (INR)
    private static final Map<String, Double> BASE_FARE = Map.of(
            "cab", 55.0,
            "premium_cab", 75.0,
            "auto", 60.0,
             "bike", 30.0
    );

    // Fixed rate per km per vehicle type
    private static final Map<String, Double> RATE_PER_KM = Map.of(
            "cab", 28.0,
            "premium_cab", 33.0,
            "auto", 15.0,
             "bike",11.0
    );


    @Override
    public String providerId() {
        return "Ola";
    }

    @Override
    public String providerName() {
        return "Ola (Mock)";
    }

    // Quick estimate → no time context → controlled randomness
    @Override
    public ProviderFare getFare(String origin, String destination, double distance) {
        return getFaresBatch(origin, destination, distance, null).get(0);
    }


    // Compare / planned trip → time-aware surge
    @Override
    public List<ProviderFare> getFaresBatch(
            String origin,
            String destination,
            double distance,
            Map<String, Object> options
    ) {
        return List.of(
                buildFare("Ola Mini", "cab", distance, 7, 14, options),
                buildFare("Ola Prime", "premium_cab", distance, 6, 12, options),
                buildFare("Ola Bike", "bike", distance, 3, 6,options),
                buildFare("Ola Auto", "auto", distance, 4, 8, options)
        );
    }

    private ProviderFare buildFare(
            String vehicleName,
            String vehicleType,
            double distance,
            int minEta,
            int maxEta,
            Map<String, Object> options
    ) {
        double baseFare = BASE_FARE.getOrDefault(vehicleType, 0.0);
        double ratePerKm = RATE_PER_KM.getOrDefault(vehicleType, 0.0);
        double distanceFare = distance * ratePerKm;

        double surgeFactor = calculateSurgeFactor(vehicleType, options);
        boolean surge = surgeFactor > 1.15;

        double finalPrice = (baseFare + distanceFare) * surgeFactor;

        return ProviderFare.builder()
                .providerId(providerId()+" : "+vehicleType)  // NEW PROVIDER ID
                .providerName(vehicleName)
                .vehicleType(vehicleType)
                .distanceKm(distance)
                .price(finalPrice)
                .etaMinutes((int) random(minEta, maxEta))
                .currency("INR")
                .isSurge(surge)
                .metadata(Map.of(
                        "baseFare", baseFare,
                        "ratePerKm", ratePerKm,
                        "distanceFare", distanceFare,
                        "surgeFactor", surgeFactor,
                        "pricingModel", "base + distance * surge",
                        "source", "osrm"
                ))
                .build();
    }

    private double calculateSurgeFactor(
            String vehicleType,
            Map<String, Object> options
    ) {
//        // No time provided → controlled randomness
//        if (options == null || !options.containsKey("departureTime")) {
//            return random(0.95, 1.15);
//        }

        try {
            LocalDateTime departureTime =LocalDateTime.now();

            String timeStr = departureTime.toString();
            int hour = Integer.parseInt(timeStr.substring(11, 13)); // yyyy-MM-ddTHH:mm

            boolean morningPeak = hour >= 8 && hour <= 11;
            boolean eveningPeak = hour >= 17 && hour <= 21;
            boolean afternoonLow = hour >= 12 && hour <= 16;

            double baseSurge;

            if (morningPeak || eveningPeak) {
                baseSurge = switch (vehicleType) {
                    case "auto" -> 1.10;
                    case "cab" -> 1.20;
                    case "premium_cab" -> 1.30;
                    case "bike" -> 1.05;
                    default -> 1.15;
                };
            } else if (afternoonLow) {
                baseSurge = switch (vehicleType) {
                    case "auto" -> 0.85;
                    case "cab" -> 1.0;
                    case "bike" -> 0.95;
                    case "premium_cab" -> 1.05;
                    default -> 1.0;
                };
            } else {
                // night / early morning
                baseSurge = switch (vehicleType) {
                    case "auto" -> 1.0;
                    case "cab" -> 1.05;
                    case "bike" -> 1.0;
                    case "premium_cab" -> 1.10;
                    default -> 1.05;
                };
            }

            // small randomness to avoid static pricing
            return baseSurge + random(-0.03, 0.03);

        } catch (Exception e) {
            return random(0.95, 1.05);
        }
    }

    private double random(double min, double max) {
        return new Random().nextDouble() * (max - min) + min;
    }
}
