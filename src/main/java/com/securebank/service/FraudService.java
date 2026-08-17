package com.securebank.service;

import com.securebank.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudService {

    public record RiskResult(int score, List<String> reasons) {}

    public RiskResult calculateRisk(long amount, String device, String location, User user) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (amount > 50_000) {
            score += 40;
            reasons.add("High Amount");
        }
        if (!user.getKnownDevices().contains(device)) {
            score += 20;
            reasons.add("New Device");
        }
        if (!user.getKnownLocations().contains(location)) {
            score += 20;
            reasons.add("New Location");
        }

        Instant now = Instant.now();
        long recentCount = user.getRecentTransferTimes().stream()
                .filter(t -> t.until(now, ChronoUnit.SECONDS) < 60)
                .count();
        if (recentCount > 3) {
            score += 20;
            reasons.add("Too Many Transfers in 1 Minute");
        }

        return new RiskResult(score, reasons);
    }
}
