package bank;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebApp {
    private static final Bank bank = Bank.load();

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> send(ex, "text/html; charset=utf-8", Page.HTML));
        server.createContext("/api", WebApp::handleApi);
        server.start();
        System.out.println("Java Bank running on port " + port);
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        String msg;
        boolean ok = true;
        try {
            Map<String, String> p = parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String path = ex.getRequestURI().getPath();
            synchronized (bank) {
                if (path.equals("/api/create")) {
                    String type = "2".equals(p.get("type")) ? "CURRENT" : "SAVINGS";
                    Account a = bank.createAccount(p.getOrDefault("name", ""), type,
                            p.getOrDefault("pin", ""), Double.parseDouble(p.getOrDefault("amount", "")));
                    bank.save();
                    msg = "Account created! Your account number is " + a.getAccountNumber();
                } else {
                    Account a = bank.login(Long.parseLong(p.getOrDefault("acc", "")), p.getOrDefault("pin", ""));
                    if (a == null) throw new IllegalArgumentException("Invalid account number or PIN.");
                    msg = perform(a, p);
                    bank.save();
                }
            }
        } catch (NumberFormatException e) {
            ok = false; msg = "Please enter valid numbers.";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ok = false; msg = e.getMessage();
        } catch (Exception e) {
            ok = false; msg = "Server error.";
        }
        String json = "{\"ok\":" + ok + ",\"message\":\"" + esc(msg) + "\"}";
        send(ex, "application/json; charset=utf-8", json);
    }

    private static String perform(Account a, Map<String, String> p) {
        String action = p.getOrDefault("action", "");
        switch (action) {
            case "login":
                return "Welcome, " + a.getHolderName() + "!";
            case "deposit":
                a.deposit(amount(p));
                return "Deposited successfully. Balance: " + money(a.getBalance());
            case "withdraw":
                a.withdraw(amount(p));
                return "Please collect your cash. Balance: " + money(a.getBalance());
            case "balance":
                return "Balance: " + money(a.getBalance());
            case "info":
                return a.getInfo();
            case "statement": {
                List<Transaction> h = a.getHistory();
                StringBuilder sb = new StringBuilder("Last 5 transactions:\n");
                for (int i = Math.max(0, h.size() - 5); i < h.size(); i++) sb.append(h.get(i)).append("\n");
                return sb.toString();
            }
            case "transfer":
                bank.transfer(a, Long.parseLong(p.getOrDefault("to", "")), amount(p));
                return "Transfer successful. Balance: " + money(a.getBalance());
            case "interest":
                return "Interest credited: " + money(a.addInterest());
            case "changepin": {
                String np = p.getOrDefault("newpin", "");
                if (!np.matches("\\d{4}")) throw new IllegalArgumentException("PIN must be 4 digits.");
                a.changePin(np);
                return "PIN changed.";
            }
            default:
                throw new IllegalArgumentException("Unknown action.");
        }
    }

    private static double amount(Map<String, String> p) {
        return Double.parseDouble(p.getOrDefault("amount", ""));
    }

    private static String money(double d) { return String.format("%.2f", d); }

    private static Map<String, String> parse(String body) {
        Map<String, String> map = new HashMap<>();
        if (body.isEmpty()) return map;
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void send(HttpExchange ex, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
