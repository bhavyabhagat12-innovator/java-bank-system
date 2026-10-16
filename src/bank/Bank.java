package bank;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Bank implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILE = "bank-data.ser";

    private final Map<Long, Account> accounts = new HashMap<>();
    private long nextAccountNumber = 1001;

    public Account createAccount(String name, String type, String pin, double openingBalance) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        if (!pin.matches("\\d{4}")) throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        double min = type.equals("SAVINGS") ? Account.MIN_BALANCE : 0;
        if (openingBalance < min)
            throw new IllegalArgumentException("Opening balance must be at least " + min);
        Account a = new Account(nextAccountNumber++, name.trim(), type, pin, openingBalance);
        accounts.put(a.getAccountNumber(), a);
        return a;
    }

    public Account login(long accNo, String pin) {
        Account a = accounts.get(accNo);
        if (a == null || !a.checkPin(pin)) return null;
        return a;
    }

    public Account find(long accNo) { return accounts.get(accNo); }

    public void transfer(Account from, long toAccNo, double amount) {
        Account to = accounts.get(toAccNo);
        if (to == null) throw new IllegalArgumentException("Destination account not found.");
        if (to == from) throw new IllegalArgumentException("Cannot transfer to the same account.");
        from.withdrawForTransfer(amount, toAccNo);
        to.receiveTransfer(amount, from.getAccountNumber());
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    public static Bank load() {
        File f = new File(FILE);
        if (!f.exists()) return new Bank();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (Bank) in.readObject();
        } catch (Exception e) {
            System.out.println("Could not read saved data, starting fresh.");
            return new Bank();
        }
    }
}
