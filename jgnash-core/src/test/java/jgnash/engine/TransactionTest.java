package jgnash.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.Test;

public class TransactionTest {

    private static final char[] PASSWORD = new char[]{};

    @Test
    public void test() {
        testBackEnd();
    }

    private static void testBackEnd() {

        String database = EngineFactory.getDefaultDatabase() + "-transaction-test.xml";

        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

            CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

            e.addCurrency(defaultCurrency);
            e.setDefaultCurrency(defaultCurrency);

            CurrencyNode cadCurrency = DefaultCurrencies.buildCustomNode("CAD");
            e.addCurrency(cadCurrency);

            Account incomeAccount = new Account(AccountType.INCOME, defaultCurrency);
            incomeAccount.setName("Income Account");
            e.addAccount(e.getRootAccount(), incomeAccount);

            Account expenseAccount = new Account(AccountType.EXPENSE, defaultCurrency);
            expenseAccount.setName("Expense Account");
            e.addAccount(e.getRootAccount(), expenseAccount);

            Account usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
            usdBankAccount.setName("USD Bank Account");
            e.addAccount(e.getRootAccount(), usdBankAccount);

            Account cadBankAccount = new Account(AccountType.BANK, cadCurrency);
            cadBankAccount.setName("CAD Bank Account");
            e.addAccount(e.getRootAccount(), cadBankAccount);

            TransactionEntry entry = new TransactionEntry();

            entry.setDebitAccount(incomeAccount);
            entry.setDebitAmount(new BigDecimal("-500.00"));

            entry.setCreditAmount(new BigDecimal("500.00"));
            entry.setCreditAccount(usdBankAccount);

            entry.setMemo("Income transaction");

            Transaction transaction = new Transaction();
            transaction.addTransactionEntry(entry);
            transaction.setPayee("Employer");

            e.addTransaction(transaction);

            assertEquals(new BigDecimal("500.00"), transaction.getAmount(usdBankAccount));
            assertEquals(new BigDecimal("500.00"), usdBankAccount.getBalance());

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
            assertTrue("passed test: ", true);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testEmptyAccount() {
        String database = EngineFactory.getDefaultDatabase() + "-empty-test.xml";

        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

            CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

            e.addCurrency(defaultCurrency);
            e.setDefaultCurrency(defaultCurrency);

            Account usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
            usdBankAccount.setName("USD Bank Account");
            e.addAccount(e.getRootAccount(), usdBankAccount);

            // ensure API does not break down

            List<Transaction> transactions = usdBankAccount.getSortedTransactionList();

            assertTrue(transactions.isEmpty());

            transactions = usdBankAccount.getTransactions(new Date(1), new Date());

            assertTrue(transactions.isEmpty());
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

}
