package com.expensetracker.model;

import java.time.LocalDate;

public abstract class Transaction {
    private long id;
    private String title;
    private double amount;
    private String category;
    private String date;
    private String notes;

    protected Transaction(long id, String title, double amount, String category, String date, String notes) {
        this.id = id;
        this.title = requireText(title, "Title");
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive finite number");
        }
        this.amount = amount;
        this.category = requireText(category, "Category");
        this.date = requireText(date, "Date");
        LocalDate.parse(date);
        this.notes = notes == null ? "" : notes.trim();
    }

    private static String requireText(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " cannot be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }

    public abstract String getType();
    public abstract double getSignedAmount();
}
