package com.securebank.service;

import com.securebank.model.Transaction;
import com.securebank.model.User;
import com.securebank.store.DataStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BankService {

    private final DataStore store;
    private final FraudService fraudService;

    public BankService(DataStore store, FraudService fraudService) {
        this.store = store;
        this.fraudService = fraudService;
    }

    public record ActionResult(boolean success, String message) {}

    // ---------------- Auth ----------------

    public ActionResult register(String name, String email, String password) {
        if (isBlank(name) || isBlank(email) || isBlank(password)) {
            return new ActionResult(false, "Please fill all fields.");
        }
        if (store.getUsers().containsKey(email)) {
            return new ActionResult(false, "An account with this email already exists.");
        }
        store.getUsers().put(email, new User(
                name, email, DataStore.hash(password), 50_000,
                DataStore.DEFAULT_DEVICE, DataStore.DEFAULT_LOCATION));
        return new ActionResult(true, "Account created for " + name + ". You can now log in.");
    }

    public ActionResult login(String email, String password) {
        User user = store.getUsers().get(email);
        if (user != null && user.getPasswordHash().equals(DataStore.hash(password))) {
            return new ActionResult(true, "Welcome, " + user.getName());
        }
        return new ActionResult(false, "Invalid credentials.");
    }

    public boolean adminLogin(String username, String password) {
        return DataStore.ADMIN_USER.equals(username) && DataStore.ADMIN_PASS.equals(password);
    }

    public Optional<User> findUser(String email) {
        return Optional.ofNullable(email == null ? null : store.getUsers().get(email));
    }

    // ---------------- Transfer + fraud decision ----------------

    public ActionResult transfer(String userEmail, String receiver, long amount, String device, String location) {
        User user = store.getUsers().get(userEmail);
        if (user == null) {
            return new ActionResult(false, "Please log in first.");
        }
        if (amount > user.getBalance()) {
            return new ActionResult(false, "Insufficient balance.");
        }

        FraudService.RiskResult risk = fraudService.calculateRisk(amount, device, location, user);
        user.getRecentTransferTimes().add(Instant.now());

        int id = store.nextTransactionId();
        Transaction.Status status = risk.score() < DataStore.RISK_THRESHOLD
                ? Transaction.Status.SUCCESS
                : Transaction.Status.PENDING_REVIEW;

        Transaction txn = new Transaction(
                id, userEmail, user.getName(), receiver, amount, device, location,
                risk.score(), risk.reasons(),
                LocalDateTime.now(ZoneId.systemDefault()), status);

        if (status == Transaction.Status.SUCCESS) {
            user.setBalance(user.getBalance() - amount);
            store.getTransactions().add(txn);
            return new ActionResult(true, "Transaction Successful — Risk Score: " + risk.score());
        } else {
            store.getTransactions().add(txn);
            store.getFraudAlerts().add(txn);
            String reasonLines = String.join(", ", risk.reasons());
            return new ActionResult(false,
                    "Suspicious Transaction. Amount: Rs " + String.format("%,d", amount) +
                    " | Risk Score: " + risk.score() +
                    " | Reasons: " + reasonLines +
                    " — under review by the bank's fraud team.");
        }
    }

    // ---------------- Customer views ----------------

    public List<Transaction> getHistory(String userEmail) {
        if (userEmail == null) return List.of();
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : store.getTransactions()) {
            if (t.getUserEmail().equals(userEmail)) list.add(t);
        }
        list.sort(Comparator.comparing(Transaction::getId).reversed());
        return list;
    }

    public List<Transaction> getAlerts(String userEmail) {
        if (userEmail == null) return List.of();
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : store.getFraudAlerts()) {
            if (t.getUserEmail().equals(userEmail)) list.add(t);
        }
        list.sort(Comparator.comparing(Transaction::getId).reversed());
        return list;
    }

    // ---------------- Admin views ----------------

    public List<Transaction> getPending() {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : store.getFraudAlerts()) {
            if (t.getStatus() == Transaction.Status.PENDING_REVIEW) list.add(t);
        }
        return list;
    }

    public List<Transaction> getResolved() {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : store.getFraudAlerts()) {
            if (t.getStatus() != Transaction.Status.PENDING_REVIEW) list.add(t);
        }
        list.sort(Comparator.comparing(Transaction::getId).reversed());
        return list;
    }

    public ActionResult decide(Integer txnId, boolean approve) {
        if (txnId == null) {
            return new ActionResult(false, "Enter a valid pending Transaction ID.");
        }
        Transaction match = null;
        for (Transaction t : store.getFraudAlerts()) {
            if (t.getId() == txnId && t.getStatus() == Transaction.Status.PENDING_REVIEW) {
                match = t;
                break;
            }
        }
        if (match == null) {
            return new ActionResult(false, "Enter a valid pending Transaction ID.");
        }

        if (approve) {
            User sender = store.getUsers().get(match.getUserEmail());
            sender.setBalance(sender.getBalance() - match.getAmount());
            match.setStatus(Transaction.Status.APPROVED);
        } else {
            match.setStatus(Transaction.Status.REJECTED);
        }

        for (Transaction t : store.getTransactions()) {
            if (t.getId() == txnId) {
                t.setStatus(match.getStatus());
            }
        }

        String msg = approve
                ? "Transaction #" + txnId + " approved and transferred."
                : "Transaction #" + txnId + " rejected. Money stays with the customer.";
        return new ActionResult(true, msg);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
