package com.securebank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Transaction {
    public enum Status {
        SUCCESS("Success"),
        PENDING_REVIEW("Blocked - Pending Review"),
        APPROVED("Approved - Transferred"),
        REJECTED("Rejected - Money Retained");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final int id;
    private final String userEmail;
    private final String userName;
    private final String receiver;
    private final long amount;
    private final String device;
    private final String location;
    private final int riskScore;
    private final List<String> reasons;
    private final LocalDateTime timestamp;
    private Status status;

    public Transaction(int id, String userEmail, String userName, String receiver, long amount,
                        String device, String location, int riskScore, List<String> reasons,
                        LocalDateTime timestamp, Status status) {
        this.id = id;
        this.userEmail = userEmail;
        this.userName = userName;
        this.receiver = receiver;
        this.amount = amount;
        this.device = device;
        this.location = location;
        this.riskScore = riskScore;
        this.reasons = reasons;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getUserName() { return userName; }
    public String getReceiver() { return receiver; }
    public long getAmount() { return amount; }
    public String getDevice() { return device; }
    public String getLocation() { return location; }
    public int getRiskScore() { return riskScore; }
    public List<String> getReasons() { return reasons; }
    public String getReasonsJoined() { return String.join(", ", reasons); }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
