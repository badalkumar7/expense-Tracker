package com.expensetracker.db;

import com.expensetracker.model.BudgetLimit;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager() throws SQLException {
        this(Path.of("expenses.db"));
    }

    public DatabaseManager(Path databasePath) throws SQLException {
        if (databasePath == null) {
            throw new IllegalArgumentException("Database path cannot be null");
        }
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        try {
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (java.io.IOException exception) {
            throw new SQLException("Could not create database directory", exception);
        }
        this.jdbcUrl = "jdbc:sqlite:" + absolutePath;
        initializeSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, title TEXT NOT NULL, "
                    + "amount REAL NOT NULL CHECK(amount > 0), category TEXT NOT NULL, date TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '')");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS budgets ("
                    + "category TEXT PRIMARY KEY, monthly_limit REAL NOT NULL CHECK(monthly_limit > 0))");
        }
    }

    public long addTransaction(Transaction transaction) throws SQLException {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        String sql = "INSERT INTO transactions(type, title, amount, category, date, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transaction.getType());
            statement.setString(2, transaction.getTitle());
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getCategory());
            statement.setString(5, transaction.getDate());
            statement.setString(6, transaction.getNotes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setId(keys.getLong(1));
                }
            }
            return transaction.getId();
        }
    }

    public boolean deleteTransaction(long id) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public ArrayList<Transaction> getAllTransactions() throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, type, title, amount, category, date, notes FROM transactions ORDER BY date DESC, id DESC";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String type = rows.getString("type");
                Transaction transaction;
                if ("INCOME".equals(type)) {
                    transaction = new Income(rows.getLong("id"), rows.getString("title"), rows.getDouble("amount"),
                            rows.getString("category"), rows.getString("date"), rows.getString("notes"));
                } else if ("EXPENSE".equals(type)) {
                    transaction = new Expense(rows.getLong("id"), rows.getString("title"), rows.getDouble("amount"),
                            rows.getString("category"), rows.getString("date"), rows.getString("notes"));
                } else {
                    throw new SQLException("Unknown transaction type in database: " + type);
                }
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    public void setCategoryBudget(String category, double monthlyLimit) throws SQLException {
        BudgetLimit budget = new BudgetLimit(category, monthlyLimit);
        String sql = "INSERT INTO budgets(category, monthly_limit) VALUES (?, ?) "
                + "ON CONFLICT(category) DO UPDATE SET monthly_limit = excluded.monthly_limit";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, budget.getCategory());
            statement.setDouble(2, budget.getMonthlyLimit());
            statement.executeUpdate();
        }
    }

    public HashMap<String, Double> getCategoryBudgets() throws SQLException {
        HashMap<String, Double> budgets = new HashMap<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("SELECT category, monthly_limit FROM budgets ORDER BY category");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) budgets.put(rows.getString("category"), rows.getDouble("monthly_limit"));
        }
        return budgets;
    }

    public void setCategoryBudgets(Map<String, Double> budgets) throws SQLException {
        for (Map.Entry<String, Double> entry : budgets.entrySet()) {
            setCategoryBudget(entry.getKey(), entry.getValue());
        }
    }
}
