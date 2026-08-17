package com.securebank.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private final String name;
    private final String email;
    private final String passwordHash;
    private long balance;
    private final Set<String> knownDevices = ConcurrentHashMap.newKeySet();
    private final Set<String> knownLocations = ConcurrentHashMap.newKeySet();
    private final List<Instant> recentTransferTimes = new CopyOnWriteArrayList<>();

    public User(String name, String email, String passwordHash, long balance,
                String defaultDevice, String defaultLocation) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.balance = balance;
        this.knownDevices.add(defaultDevice);
        this.knownLocations.add(defaultLocation);
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
    public Set<String> getKnownDevices() { return knownDevices; }
    public Set<String> getKnownLocations() { return knownLocations; }
    public List<Instant> getRecentTransferTimes() { return recentTransferTimes; }
}
