package com.expensetracker.model;

public final class BudgetLimit {
    private final String category;
    private final double monthlyLimit;

    public BudgetLimit(String category, double monthlyLimit) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be blank");
        }
        if (!Double.isFinite(monthlyLimit) || monthlyLimit <= 0) {
            throw new IllegalArgumentException("Monthly limit must be a positive finite number");
        }
        this.category = category.trim();
        this.monthlyLimit = monthlyLimit;
    }

    public String getCategory() {
        return category;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }
}
