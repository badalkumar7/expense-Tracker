package com.expensetracker.model;

public final class Expense extends Transaction {
    public Expense(long id, String title, double amount, String category, String date, String notes) {
        super(id, title, amount, category, date, notes);
    }

    @Override
    public String getType() {
        return "EXPENSE";
    }

    @Override
    public double getSignedAmount() {
        return -getAmount();
    }
}
