package bank;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final double MIN_BALANCE = 500.0;          // savings account rule
    public static final double DEFAULT_DAILY_LIMIT = 20000.0;
    public static final double INTEREST_RATE = 0.04;         // 4% per year (savings)

    private final long accountNumber;
    private final String holderName;
    private final String type;            // "SAVINGS" or "CURRENT"
    private String pinHash;
    private double balance;
    private final double dailyLimit;
    private double withdrawnToday;
    private LocalDate lastWithdrawDate;
    private final LocalDate openedOn;
    private final List<Transaction> history = new ArrayList<>();

    public Account(long accountNumber, String holderName, String type, String pin, double openingBalance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.type = type;
        this.pinHash = hash(pin);
        this.balance = openingBalance;
        this.dailyLimit = DEFAULT_DAILY_LIMIT;
        this.openedOn = LocalDate.now();
        this.lastWithdrawDate = LocalDate.now();
        history.add(new Transaction("ACCOUNT OPENED", openingBalance, balance));
    }

    private static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkPin(String pin) { return pinHash.equals(hash(pin)); }

    public void changePin(String newPin) {
        pinHash = hash(newPin);
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        balance += amount;
        history.add(new Transaction("DEPOSIT", amount, balance));
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        resetDailyCounterIfNewDay();
        if (withdrawnToday + amount > dailyLimit) {
            throw new IllegalArgumentException(String.format(
                "Daily withdrawal limit exceeded. You can still withdraw %.2f today.", dailyLimit - withdrawnToday));
        }
        double minAllowed = type.equals("SAVINGS") ? MIN_BALANCE : 0;
        if (balance - amount < minAllowed) {
            throw new IllegalArgumentException(String.format(
                "Insufficient balance. Minimum balance of %.2f must be maintained.", minAllowed));
        }
        balance -= amount;
        withdrawnToday += amount;
        history.add(new Transaction("WITHDRAW", amount, balance));
    }

    /** Used for transfers: records a different label. */
    public void withdrawForTransfer(double amount, long toAcc) {
        withdraw(amount);
        history.remove(history.size() - 1);
        history.add(new Transaction("TRANSFER OUT>" + toAcc, amount, balance));
    }

    public void receiveTransfer(double amount, long fromAcc) {
        balance += amount;
        history.add(new Transaction("TRANSFER IN<" + fromAcc, amount, balance));
    }

    public double addInterest() {
        if (!type.equals("SAVINGS")) throw new IllegalStateException("Interest is only for savings accounts.");
        double interest = balance * INTEREST_RATE / 12.0;   // one month of interest
        balance += interest;
        history.add(new Transaction("INTEREST", interest, balance));
        return interest;
    }

    private void resetDailyCounterIfNewDay() {
        if (!lastWithdrawDate.equals(LocalDate.now())) {
            withdrawnToday = 0;
            lastWithdrawDate = LocalDate.now();
        }
    }

    public double getRemainingLimit() {
        resetDailyCounterIfNewDay();
        return dailyLimit - withdrawnToday;
    }

    public double getBalance() { return balance; }
    public long getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public List<Transaction> getHistory() { return history; }

    public String getInfo() {
        return "Account Number : " + accountNumber + "\n"
             + "Holder Name    : " + holderName + "\n"
             + "Account Type   : " + type + "\n"
             + "Opened On      : " + openedOn + "\n"
             + String.format("Balance        : %.2f", balance) + "\n"
             + String.format("Daily Limit    : %.2f (remaining today: %.2f)", dailyLimit, getRemainingLimit());
    }

    public void display() {
        System.out.println("-----------------------------------");
        System.out.println(getInfo());
        System.out.println("-----------------------------------");
    }
}
