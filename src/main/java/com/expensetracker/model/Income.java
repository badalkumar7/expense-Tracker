package com.expensetracker.model;

public final class Income extends Transaction {
    public Income(long id, String title, double amount, String category, String date, String notes) {
        super(id, title, amount, category, date, notes);
    }

    @Override
    public String getType() {
        return "INCOME";
    }

    @Override
    public double getSignedAmount() {
        return getAmount();
    }
}
