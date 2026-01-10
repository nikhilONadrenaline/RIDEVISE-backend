package com.transportoptimizer.Services;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmissionCalculationService {

    // base kg CO2 per km (no traffic)
    private static final Map<String, Double> BASE_CO2_PER_KM = Map.of(
            "walk", 0.0,
            "metro", 0.02,
            "bus", 0.05,
            "auto", 0.07,
            "bike", 0.08,
            "cab", 0.12,
            "premium_cab", 0.15
    );

    public double calculate(
            String vehicleType,
            double distanceKm,
            double surgeFactor
    ) {
        double base = BASE_CO2_PER_KM.getOrDefault(vehicleType, 0.1);

        // walk & metro not affected by traffic
        if ("walk".equals(vehicleType) || "metro".equals(vehicleType)) {
            return round(distanceKm * base);
        }

        // traffic proxy via surge
        // double trafficMultiplier = 1 + ((surgeFactor - 1) * 0.6);

        return round(distanceKm * base);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

