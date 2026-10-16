package bank;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final Bank bank = Bank.load();

    public static void main(String[] args) {
        System.out.println("===== WELCOME TO JAVA BANK =====");
        while (true) {
            System.out.println("\n1. Create Account\n2. Login\n3. Exit");
            int ch = readInt("Choose: ");
            switch (ch) {
                case 1 -> createAccount();
                case 2 -> login();
                case 3 -> { bank.save(); System.out.println("Thank you! Goodbye."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void createAccount() {
        try {
            System.out.print("Full name: ");
            String name = sc.nextLine();
            int t = readInt("Account type (1=Savings, 2=Current): ");
            String type = (t == 2) ? "CURRENT" : "SAVINGS";
            System.out.print("Set a 4-digit PIN: ");
            String pin = sc.nextLine().trim();
            double amt = readDouble("Opening balance: ");
            Account a = bank.createAccount(name, type, pin, amt);
            bank.save();
            System.out.println("Account created! Your account number is " + a.getAccountNumber());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void login() {
        long accNo = (long) readDouble("Account number: ");
        System.out.print("PIN: ");
        String pin = sc.nextLine().trim();
        Account a = bank.login(accNo, pin);
        if (a == null) { System.out.println("Invalid account number or PIN."); return; }
        System.out.println("Welcome, " + a.getHolderName() + "!");
        accountMenu(a);
    }

    private static void accountMenu(Account a) {
        while (true) {
            System.out.println("\n1. Deposit\n2. Withdraw\n3. Check Balance\n4. Account Info"
                + "\n5. Mini Statement\n6. Transfer Money\n7. Add Monthly Interest (Savings)"
                + "\n8. Change PIN\n9. Logout");
            int ch = readInt("Choose: ");
            try {
                switch (ch) {
                    case 1 -> { a.deposit(readDouble("Amount: ")); System.out.println("Deposited successfully."); }
                    case 2 -> { a.withdraw(readDouble("Amount: ")); System.out.println("Please collect your cash."); }
                    case 3 -> System.out.printf("Balance: %.2f%n", a.getBalance());
                    case 4 -> a.display();
                    case 5 -> showStatement(a);
                    case 6 -> {
                        long to = (long) readDouble("Destination account number: ");
                        bank.transfer(a, to, readDouble("Amount: "));
                        System.out.println("Transfer successful.");
                    }
                    case 7 -> System.out.printf("Interest credited: %.2f%n", a.addInterest());
                    case 8 -> {
                        System.out.print("New 4-digit PIN: ");
                        String p = sc.nextLine().trim();
                        if (!p.matches("\\d{4}")) throw new IllegalArgumentException("PIN must be 4 digits.");
                        a.changePin(p);
                        System.out.println("PIN changed.");
                    }
                    case 9 -> { bank.save(); System.out.println("Logged out."); return; }
                    default -> System.out.println("Invalid choice.");
                }
                bank.save();
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void showStatement(Account a) {
        var h = a.getHistory();
        System.out.println("---- Last 5 transactions ----");
        for (int i = Math.max(0, h.size() - 5); i < h.size(); i++) System.out.println(h.get(i));
    }

    private static int readInt(String msg) {
        while (true) {
            System.out.print(msg);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    private static double readDouble(String msg) {
        while (true) {
            System.out.print(msg);
            try { return Double.parseDouble(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }
}
