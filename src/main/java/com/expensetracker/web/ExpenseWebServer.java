package com.expensetracker.web;

import com.expensetracker.db.DatabaseManager;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import com.expensetracker.service.BudgetService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ExpenseWebServer {
    private static final Gson GSON = new Gson();
    private final HttpServer server;
    private final DatabaseManager database;
    private final BudgetService service;

    public ExpenseWebServer() throws IOException, java.sql.SQLException {
        this(8080, new DatabaseManager());
    }

    public ExpenseWebServer(int port, DatabaseManager database) throws IOException, java.sql.SQLException {
        this.database = database;
        this.service = new BudgetService(database);
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        this.server.createContext("/api", this::handleApi);
        this.server.createContext("/", this::serveDashboard);
        this.server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    private void serveDashboard(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method not allowed", "text/plain; charset=utf-8");
            return;
        }
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
            return;
        }
        try (InputStream page = ExpenseWebServer.class.getResourceAsStream("/web/index.html")) {
            if (page == null) {
                sendText(exchange, 500, "Dashboard resource is missing", "text/plain; charset=utf-8");
                return;
            }
            byte[] bytes = page.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("/api/transactions".equals(path)) {
                if ("GET".equals(method)) {
                    ArrayList<JsonObject> response = new ArrayList<>();
                    for (Transaction transaction : service.getTransactions()) {
                        response.add(transactionJson(transaction));
                    }
                    sendJson(exchange, 200, response);
                } else if ("POST".equals(method)) {
                    Transaction transaction = transactionFromJson(readJson(exchange));
                    service.addTransaction(transaction);
                    sendJson(exchange, 201, transactionJson(transaction));
                } else if ("DELETE".equals(method)) {
                    String idValue = queryParameters(exchange.getRequestURI().getRawQuery()).get("id");
                    if (idValue == null) {
                        throw new IllegalArgumentException("Missing query parameter: id");
                    }
                    long id;
                    try {
                        id = Long.parseLong(idValue);
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("id must be a whole number");
                    }
                    sendJson(exchange, 200, Map.of("deleted", service.deleteTransaction(id)));
                } else {
                    methodNotAllowed(exchange);
                }
            } else if ("/api/budgets".equals(path)) {
                if ("GET".equals(method)) {
                    sendJson(exchange, 200, database.getCategoryBudgets());
                } else if ("POST".equals(method)) {
                    JsonObject body = readJson(exchange);
                    database.setCategoryBudget(requiredString(body, "category"), requiredDouble(body, "monthlyLimit"));
                    sendJson(exchange, 200, database.getCategoryBudgets());
                } else {
                    methodNotAllowed(exchange);
                }
            } else if ("/api/sample-data".equals(path) && "POST".equals(method)) {
                loadSampleData();
                sendJson(exchange, 200, Map.of("transactions", service.getTransactions().size(), "budgets", database.getCategoryBudgets()));
            } else if ("/api/export-csv".equals(path) && "GET".equals(method)) {
                java.io.StringWriter text = new java.io.StringWriter();
                service.writeCsv(new java.io.BufferedWriter(text));
                byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=expenses.csv");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                sendJson(exchange, 404, Map.of("error", "API route not found"));
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage() == null ? "Invalid request" : exception.getMessage()));
        } catch (java.time.DateTimeException exception) {
            sendJson(exchange, 400, Map.of("error", "Date must use YYYY-MM-DD format"));
        } catch (java.sql.SQLException exception) {
            sendJson(exchange, 500, Map.of("error", "Database operation failed"));
        } catch (Exception exception) {
            sendJson(exchange, 500, Map.of("error", "Request could not be completed"));
        }
    }

    private void loadSampleData() throws java.sql.SQLException {
        if (!service.getTransactions().isEmpty()) {
            return;
        }
        String today = LocalDate.now().toString();
        service.addTransaction(new Income(0, "Monthly salary", 4200, "Salary", today, "Sample income"));
        service.addTransaction(new Expense(0, "Apartment rent", 1450, "Housing", today, "Sample expense"));
        service.addTransaction(new Expense(0, "Weekly groceries", 186.45, "Food", today, "Sample expense"));
        service.addTransaction(new Expense(0, "Train pass", 92, "Transport", today, "Sample expense"));
        database.setCategoryBudget("Housing", 1600);
        database.setCategoryBudget("Food", 150);
        database.setCategoryBudget("Transport", 120);
    }

    private static Transaction transactionFromJson(JsonObject body) {
        String type = requiredString(body, "type").toUpperCase(java.util.Locale.ROOT);
        long id = body.has("id") && body.get("id").isJsonPrimitive() ? body.get("id").getAsLong() : 0;
        String title = requiredString(body, "title");
        double amount = requiredDouble(body, "amount");
        String category = requiredString(body, "category");
        String date = requiredString(body, "date");
        String notes = body.has("notes") && !body.get("notes").isJsonNull() ? body.get("notes").getAsString() : "";
        if ("INCOME".equals(type)) {
            return new Income(id, title, amount, category, date, notes);
        }
        if ("EXPENSE".equals(type)) {
            return new Expense(id, title, amount, category, date, notes);
        }
        throw new IllegalArgumentException("Type must be INCOME or EXPENSE");
    }

    private static JsonObject transactionJson(Transaction transaction) {
        JsonObject json = new JsonObject();
        json.addProperty("id", transaction.getId());
        json.addProperty("type", transaction.getType());
        json.addProperty("title", transaction.getTitle());
        json.addProperty("amount", transaction.getAmount());
        json.addProperty("category", transaction.getCategory());
        json.addProperty("date", transaction.getDate());
        json.addProperty("notes", transaction.getNotes());
        return json;
    }

    private static JsonObject readJson(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return GSON.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (com.google.gson.JsonParseException exception) {
            throw new IllegalArgumentException("Request body must be valid JSON");
        }
    }

    private static String requiredString(JsonObject body, String property) {
        if (body == null || !body.has(property) || body.get(property).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + property);
        }
        try {
            return body.get(property).getAsString();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Field must be text: " + property);
        }
    }

    private static double requiredDouble(JsonObject body, String property) {
        if (body == null || !body.has(property) || body.get(property).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + property);
        }
        try {
            return body.get(property).getAsDouble();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Field must be numeric: " + property);
        }
    }

    private static Map<String, String> queryParameters(String query) {
        HashMap<String, String> parameters = new HashMap<>();
        if (query == null || query.isBlank()) return parameters;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            parameters.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return parameters;
    }

    private static void methodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        sendText(exchange, status, GSON.toJson(value), "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String value, String contentType) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
