package com.expensetracker.service;

import com.expensetracker.db.DatabaseManager;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetService {
    private final DatabaseManager database;
    private final ArrayList<Transaction> transactions;

    public BudgetService(DatabaseManager database) throws java.sql.SQLException {
        if (database == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }
        this.database = database;
        this.transactions = new ArrayList<>(database.getAllTransactions());
    }

    public synchronized Transaction addTransaction(Transaction transaction) throws java.sql.SQLException {
        database.addTransaction(transaction);
        transactions.add(0, transaction);
        return transaction;
    }

    public synchronized boolean deleteTransaction(long id) throws java.sql.SQLException {
        boolean deleted = database.deleteTransaction(id);
        if (deleted) {
            for (int i = transactions.size() - 1; i >= 0; i--) {
                if (transactions.get(i).getId() == id) {
                    transactions.remove(i);
                }
            }
        }
        return deleted;
    }

    public synchronized ArrayList<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public synchronized double getTotalIncome() {
        double total = 0;
        for (Transaction transaction : transactions) {
            if (transaction instanceof Income) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    public synchronized double getTotalExpenses() {
        double total = 0;
        for (Transaction transaction : transactions) {
            if (transaction instanceof Expense) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    public synchronized double getNetBalance() {
        return getTotalIncome() - getTotalExpenses();
    }

    public synchronized HashMap<String, Double> getCategoryExpenseTotals() {
        HashMap<String, Double> totals = new HashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction instanceof Expense) {
                String category = transaction.getCategory();
                double total = totals.containsKey(category) ? totals.get(category) : 0;
                totals.put(category, total + transaction.getAmount());
            }
        }
        return totals;
    }

    public synchronized List<String> getBudgetAlerts() throws java.sql.SQLException {
        return getBudgetAlerts(YearMonth.now());
    }

    public synchronized List<String> getBudgetAlerts(YearMonth period) throws java.sql.SQLException {
        if (period == null) {
            throw new IllegalArgumentException("Period cannot be null");
        }
        HashMap<String, Double> monthSpending = new HashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction instanceof Expense && YearMonth.from(LocalDate.parse(transaction.getDate())).equals(period)) {
                String category = transaction.getCategory();
                double spent = monthSpending.containsKey(category) ? monthSpending.get(category) : 0;
                monthSpending.put(category, spent + transaction.getAmount());
            }
        }
        ArrayList<String> alerts = new ArrayList<>();
        HashMap<String, Double> budgets = database.getCategoryBudgets();
        for (Map.Entry<String, Double> budget : budgets.entrySet()) {
            String category = budget.getKey();
            double spent = monthSpending.containsKey(category) ? monthSpending.get(category) : 0;
            if (spent > budget.getValue()) {
                alerts.add(String.format("%s is over budget by %.2f (%.2f spent of %.2f)",
                        category, spent - budget.getValue(), spent, budget.getValue()));
            }
        }
        return alerts;
    }

    public synchronized void exportCsv(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeCsv(writer);
        }
    }

    public synchronized void writeCsv(BufferedWriter writer) throws IOException {
        writer.write("id,type,title,amount,category,date,notes");
        writer.newLine();
        for (Transaction transaction : transactions) {
            writer.write(transaction.getId() + "," + csv(transaction.getType()) + "," + csv(transaction.getTitle()) + ","
                    + transaction.getAmount() + "," + csv(transaction.getCategory()) + "," + csv(transaction.getDate())
                    + "," + csv(transaction.getNotes()));
            writer.newLine();
        }
        writer.flush();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
