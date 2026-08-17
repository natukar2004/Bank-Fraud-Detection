package com.securebank.store;

import com.securebank.model.Transaction;
import com.securebank.model.User;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory "database". Fine for a demo; resets when the container restarts.
 * Mirrors USERS / TRANSACTIONS / FRAUD_ALERTS / NEXT_ID from app.py.
 */
@Component
public class DataStore {

    public static final String DEFAULT_DEVICE = "Device-Home-Laptop";
    public static final String DEFAULT_LOCATION = "Mumbai";
    public static final List<String> DEVICE_OPTIONS = List.of(
            "Device-Home-Laptop", "Device-Office-PC", "Device-Unknown-Phone");
    public static final List<String> LOCATION_OPTIONS = List.of(
            "Mumbai", "Delhi", "Bengaluru", "Unknown City");
    public static final int RISK_THRESHOLD = 70;
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "admin123";

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final List<Transaction> fraudAlerts = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @PostConstruct
    public void seedDemoUser() {
        users.put("rahul@gmail.com", new User(
                "Rahul", "rahul@gmail.com", hash("password123"), 50_000,
                DEFAULT_DEVICE, DEFAULT_LOCATION));
    }

    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, User> getUsers() { return users; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<Transaction> getFraudAlerts() { return fraudAlerts; }
    public int nextTransactionId() { return nextId.getAndIncrement(); }
}
