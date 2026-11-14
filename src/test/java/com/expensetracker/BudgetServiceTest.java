package com.expensetracker;

import com.expensetracker.db.DatabaseManager;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import com.expensetracker.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager database() throws Exception {
        return new DatabaseManager(temporaryDirectory.resolve("test-expenses.db"));
    }

    @Test
    void incomeAndExpenseHaveCorrectInheritanceAndSignedAmounts() {
        Transaction income = new Income(0, "Pay", 1200, "Salary", "2026-11-01", "Monthly pay");
        Transaction expense = new Expense(0, "Lunch", 18.50, "Food", "2026-11-02", "");

        assertInstanceOf(Transaction.class, income);
        assertInstanceOf(Transaction.class, expense);
        assertEquals("INCOME", income.getType());
        assertEquals(1200, income.getSignedAmount(), 0.001);
        assertEquals("EXPENSE", expense.getType());
        assertEquals(-18.50, expense.getSignedAmount(), 0.001);
    }

    @Test
    void sqliteSupportsTransactionAndBudgetCrud() throws Exception {
        DatabaseManager database = database();
        Income income = new Income(0, "Pay", 2500, "Salary", "2026-11-01", "");
        Expense expense = new Expense(0, "Market", 64.25, "Food", "2026-11-02", "Weekly shop");
        long incomeId = database.addTransaction(income);
        long expenseId = database.addTransaction(expense);

        ArrayList<Transaction> saved = database.getAllTransactions();
        assertEquals(2, saved.size());
        assertEquals(expenseId, saved.getFirst().getId());
        assertInstanceOf(Expense.class, saved.getFirst());
        assertEquals("Weekly shop", saved.getFirst().getNotes());
        assertTrue(database.deleteTransaction(incomeId));
        assertFalse(database.deleteTransaction(incomeId));
        assertEquals(1, database.getAllTransactions().size());

        database.setCategoryBudget("Food", 300);
        database.setCategoryBudget("Food", 325);
        database.setCategoryBudget("Transport", 90);
        assertEquals(325, database.getCategoryBudgets().get("Food"), 0.001);
        assertEquals(90, database.getCategoryBudgets().get("Transport"), 0.001);
    }

    @Test
    void serviceComputesIncomeExpenseBalanceAndCategoryTotals() throws Exception {
        BudgetService service = new BudgetService(database());
        service.addTransaction(new Income(0, "Pay", 3200, "Salary", "2026-11-01", ""));
        service.addTransaction(new Expense(0, "Groceries", 185.30, "Food", "2026-11-03", ""));
        service.addTransaction(new Expense(0, "Coffee", 14.70, "Food", "2026-11-04", ""));
        service.addTransaction(new Expense(0, "Metro", 55, "Transport", "2026-11-05", ""));

        assertEquals(3200, service.getTotalIncome(), 0.001);
        assertEquals(255, service.getTotalExpenses(), 0.001);
        assertEquals(2945, service.getNetBalance(), 0.001);
        assertEquals(200, service.getCategoryExpenseTotals().get("Food"), 0.001);
        assertEquals(55, service.getCategoryExpenseTotals().get("Transport"), 0.001);
    }

    @Test
    void budgetAlertsCompareCurrentMonthSpendingAgainstLimits() throws Exception {
        DatabaseManager database = database();
        BudgetService service = new BudgetService(database);
        YearMonth period = YearMonth.of(2026, 11);
        service.addTransaction(new Expense(0, "Groceries", 180, "Food", period.atDay(3).toString(), ""));
        service.addTransaction(new Expense(0, "Market", 50, "Food", period.atDay(8).toString(), ""));
        service.addTransaction(new Expense(0, "Last month", 1000, "Travel", "2026-10-15", ""));
        database.setCategoryBudget("Food", 200);
        database.setCategoryBudget("Travel", 800);

        List<String> alerts = service.getBudgetAlerts(period);
        assertEquals(1, alerts.size());
        assertTrue(alerts.getFirst().contains("Food is over budget by 30.00"));
    }

    @Test
    void csvExportIncludesHeadersAndEscapesFields() throws Exception {
        BudgetService service = new BudgetService(database());
        service.addTransaction(new Expense(0, "Lunch, with team", 22.5, "Food", "2026-11-14", "Said \"thanks\""));
        StringWriter output = new StringWriter();
        service.writeCsv(new BufferedWriter(output));
        String csv = output.toString();

        assertTrue(csv.startsWith("id,type,title,amount,category,date,notes"));
        assertTrue(csv.contains("EXPENSE,\"Lunch, with team\",22.5,Food,2026-11-14,\"Said \"\"thanks\"\"\""));
    }

    @Test
    void serviceDeleteUpdatesStoredAndInMemoryTransactions() throws Exception {
        BudgetService service = new BudgetService(database());
        Transaction expense = service.addTransaction(new Expense(0, "Taxi", 24, "Transport", LocalDate.now().toString(), ""));

        assertEquals(1, service.getTransactions().size());
        assertTrue(service.deleteTransaction(expense.getId()));
        assertTrue(service.getTransactions().isEmpty());
        assertEquals(0, service.getTotalExpenses(), 0.001);
    }
}
