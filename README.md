# Smart Expense and Budget Tracker

This Java 21 web application tracks income, expenses, category budgets, and monthly budget alerts. It uses SQLite for storage and serves a browser dashboard at `http://localhost:8080`.

## Team Members
| Name | Roll No. | Assigned Module |
| :--- | :--- | :--- |
| Badal Kumar | 2025UIT3109 | Project Setup & OOP Models & Web Dashboard UI |
| Vikas | 2025UIT3093 | SQLite JDBC Database & Unit Testing |
| Gaurav|2025UIT3122 | Budget Calculations & CSV Export |
| [Member 4 Name] | [Roll No.] | Java HttpServer & API Routing |


## Screenshots

![Smart Expense and Budget Tracker Dashboard](image.png?raw=true)


## Key Features
- Record and delete income and expense transactions across categories
- Set monthly spending limits per category with automatic budget alert warnings
- View summary KPIs (total income, total expenses, remaining balance) and a category breakdown chart
- Search transactions by keyword and filter by type (Income / Expense)
- Load sample data for quick testing and export all records to CSV

## Project Structure
- `com.expensetracker.model` — Abstract `Transaction` class with `Income`, `Expense`, and `BudgetLimit` models
- `com.expensetracker.db` — `DatabaseManager` handling SQLite JDBC connection and CRUD operations
- `com.expensetracker.service` — `BudgetService` for balance totals, category aggregation, alerts, and CSV export
- `com.expensetracker.web` — Built-in Java `HttpServer` (`ExpenseWebServer`) and JSON API routes
- `src/main/resources/web/index.html` — Browser dashboard interface

## Technologies Used
- **Language:** Java 21 (OOP, Collections, File I/O, built-in `HttpServer`)
- **Database:** SQLite (`sqlite-jdbc`)
- **Libraries:** Gson (JSON serialization), JUnit 5 (unit testing)
- **Build Tool:** Apache Maven

## How to Run
- **Using the bundled launcher:** Run `run.bat` to start the server and open `http://localhost:8080` in your browser.
- **Using system Maven:** Run `mvn compile exec:java` from the project root directory.

Transactions and budgets are stored locally in `expenses.db` in the project directory.

## Running Unit Tests
Using the bundled Maven setup:
```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd "-Dmaven.repo.local=.m2" test
