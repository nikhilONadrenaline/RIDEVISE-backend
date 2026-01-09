package com.transportoptimizer.provider.impl;

import com.transportoptimizer.entity.ProviderFare;
import com.transportoptimizer.provider.ProviderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class UberMockClient implements ProviderClient {

    private static final Map<String, Double> BASE_FARE = Map.of(
            "cab", 56.0,
            "premium_cab", 70.0,
            "auto", 40.0,
            "bike", 22.0
    );

    private static final Map<String, Double> RATE_PER_KM = Map.of(
            "cab", 25.0,
            "premium_cab", 30.0,
            "auto", 15.2,
            "bike", 8.0
    );

    private static final Map<String, Double> SHORT_TRIP_BASE_FARE = Map.of(
            "bike", 10.0,
            "auto", 35.0,
            "cab", 45.0,
            "premium_cab", 70.0
    );

    private static final double SHORT_TRIP_DISTANCE_KM = 4.0;

    @Override
    public String providerId() {
        return "Uber";
    }

    @Override
    public String providerName() {
        return "Uber (Mock)";
    }

    // Quick estimate → no time context
    @Override
    public ProviderFare getFare(String origin, String destination, double distance) {
        return buildFare("Uber Go", "cab", distance, 6, 12, null);
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
                buildFare("Uber Go", "cab", distance, 6, 12, options),
                buildFare("Uber Premier", "premium_cab", distance, 6, 12, options),
                buildFare("Uber Auto", "auto", distance, 4, 8, options),
                buildFare("Uber Moto", "bike", distance, 3, 6, options)
        );
    }

    private double resolveBaseFare(String vehicleType, double distanceKm) {
        if (distanceKm <= SHORT_TRIP_DISTANCE_KM) {
            return SHORT_TRIP_BASE_FARE.getOrDefault(
                    vehicleType,
                    BASE_FARE.getOrDefault(vehicleType, 0.0)
            );
        }
        return BASE_FARE.getOrDefault(vehicleType, 0.0);
    }

    private ProviderFare buildFare(
            String name,
            String vehicleType,
            double distance,
            int minEta,
            int maxEta,
            Map<String, Object> options
    ) {
        double baseFare = resolveBaseFare(vehicleType, distance);
        double ratePerKm = RATE_PER_KM.getOrDefault(vehicleType, 0.0);
        double distanceFare = distance * ratePerKm;

        double surgeFactor = calculateSurgeFactor(vehicleType, options);
        boolean surge = surgeFactor > 1.2;

        double finalPrice = (baseFare + distanceFare) * surgeFactor;

        return ProviderFare.builder()
                .providerId(providerId()+" : "+ vehicleType)
                .providerName(providerName())
                .vehicleType(vehicleType)
                .distanceKm(distance)
                .price(finalPrice)
                .etaMinutes((int) random(minEta, maxEta))
                .currency("INR")
                .isSurge(surge)
                .metadata(Map.of(
                        "productName", name,
                        "baseFare", baseFare,
                        "ratePerKm", ratePerKm,
                        "distanceFare", distanceFare,
                        "surgeFactor", surgeFactor,
                        "pricingModel", "base + distance * surge",
                        "source", "mock"
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
                    case "bike" -> 1.08;
                    case "auto" -> 1.15;
                    case "cab" -> 1.22;
                    case "premium_cab" -> 1.35;
                    default -> 1.25;
                };
            } else if (afternoonLow) {
                baseSurge = switch (vehicleType) {
                    case "bike" -> 0.85;
                    case "auto" -> 1.0;
                    case "cab" -> 1.05;
                    case "premium_cab" -> 1.10;
                    default -> 1.0;
                };
            } else {
                // night / early morning
                baseSurge = switch (vehicleType) {
                    case "bike" -> 1.08;
                    case "auto" -> 1.12;
                    case "cab" -> 1.18;
                    case "premium_cab" -> 1.20;
                    default -> 1.05;
                };
            }

            // slight randomness to avoid static pricing
            return baseSurge + random(-0.01, 0.04);

        } catch (Exception e) {
            return random(0.95, 1.05);
        }
    }

    private double random(double min, double max) {
        return new Random().nextDouble() * (max - min) + min;
    }
}
