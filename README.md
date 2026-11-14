Smart Expense and Budget Tracker

This Java 21 web application tracks income, expenses, category budgets, and monthly budget alerts. It uses SQLite for storage and serves a browser dashboard at http://localhost:8080.

Run run.bat to start the application. The dashboard includes transaction entry, budget settings, a category chart, search and type filters, sample data, and CSV export.

Run the unit tests with:

    .\tools\apache-maven-3.9.9\bin\mvn.cmd "-Dmaven.repo.local=.m2" test

Transactions and budgets are stored in expenses.db in the project directory.
