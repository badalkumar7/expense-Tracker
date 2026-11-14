package com.expensetracker;

import com.expensetracker.web.ExpenseWebServer;

import java.awt.Desktop;
import java.net.URI;

public class Main {
    public static void main(String[] args) throws Exception {
        ExpenseWebServer server = new ExpenseWebServer();
        server.start();
        URI address = URI.create("http://localhost:8080");
        System.out.println("Smart Expense & Budget Tracker is running at " + address);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(address);
            } catch (Exception exception) {
                System.err.println("Could not open the default browser: " + exception.getMessage());
            }
        }
    }
}
