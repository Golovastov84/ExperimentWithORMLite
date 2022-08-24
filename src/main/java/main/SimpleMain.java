package main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Основной пример подпрограммы, показывающий, как выполнять основные операции с пакетом.
 *
 * <p>
 * <b>ПРИМЕЧАНИЕ.</b> Мы используем утверждения в нескольких местах для проверки результатов, но если бы это был настоящий производственный код, мы
 * будет иметь правильную обработку ошибок.
 * </p>
 */
public class SimpleMain {

    // мы используем базу данных SQLite в памяти
    private final static String DATABASE_URL = "jdbc:sqlite:src/main/resources/account.db";

    private Dao<Account, Integer> accountDao;

    public static void main(String[] args) throws Exception {
        // превратить наш статический метод в экземпляр Main
        new SimpleMain().doMain(args);
    }

    private void doMain(String[] args) throws Exception {
        ConnectionSource connectionSource = null;
        try {
            // создадим наш источник данных для базы данных
            connectionSource = new JdbcConnectionSource(DATABASE_URL);
            // настроить нашу базу данных и DAOs
            setupDatabase(connectionSource);
            // читать и записывать некоторые данные
            readWriteData();
            // сделать кучу массовых операций
            readWriteBunch();
            // показать, как использовать объект SelectArg
            useSelectArgFeature();
            // показать, как использовать объект SelectArg
            useTransactions(connectionSource);
            System.out.println("\n\nIt seems to have worked\n\n");
        } finally {
            // destroy the data source which should close underlying connections
            if (connectionSource != null) {
                connectionSource.close();
            }
        }
    }

    /**
     * Setup our database and DAOs
     */
    private void setupDatabase(ConnectionSource connectionSource) throws Exception {

        accountDao = DaoManager.createDao(connectionSource, Account.class);

        // if you need to create the table
        TableUtils.createTable(connectionSource, Account.class);
    }

    /**
     * Read and write some example data.
     */
    private void readWriteData() throws Exception {
        // create an instance of Account
        String name = "Jim Coakley";
        Account account = new Account(name);

        // persist the account object to the database
        accountDao.create(account);
        int id = account.getId();
        verifyDb(id, account);

        // assign a password
        account.setPassword("_secret");
        // update the database after changing the object
        accountDao.update(account);
        verifyDb(id, account);

        // query for all items in the database
        List<Account> accounts = accountDao.queryForAll();
        assertEquals("Should have found 1 account matching our query", 1, accounts.size());
        verifyAccount(account, accounts.get(0));

        // loop through items in the database
        int accountC = 0;
        for (Account account2 : accountDao) {
            verifyAccount(account, account2);
            accountC++;
        }
        assertEquals("Should have found 1 account in for loop", 1, accountC);

        // construct a query using the QueryBuilder
        QueryBuilder<Account, Integer> statementBuilder = accountDao.queryBuilder();
        // shouldn't find anything: name LIKE 'hello" does not match our account
        statementBuilder.where().like(Account.NAME_FIELD_NAME, "hello");
        accounts = accountDao.query(statementBuilder.prepare());
        assertEquals("Should not have found any accounts matching our query", 0, accounts.size());

        // should find our account: name LIKE 'Jim%' should match our account
        statementBuilder.where().like(Account.NAME_FIELD_NAME, name.substring(0, 3) + "%");
        accounts = accountDao.query(statementBuilder.prepare());
        assertEquals("Should have found 1 account matching our query", 1, accounts.size());
        verifyAccount(account, accounts.get(0));

        // delete the account since we are done with it
        accountDao.delete(account);
        // we shouldn't find it now
        assertNull("account was deleted, shouldn't find any", accountDao.queryForId(id));
    }

    /**
     * Example of reading and writing a large(r) number of objects.
     */
    private void readWriteBunch() throws Exception {

        Map<String, Account> accounts = new HashMap<String, Account>();
        for (int i = 1; i <= 100; i++) {
            String name = Integer.toString(i);
            Account account = new Account(name);
            // сохранить объект учетной записи в базе данных, он должен вернуть 1
            accountDao.create(account);
            accounts.put(name, account);
        }

        // запрос для всех элементов в базе данных
        List<Account> all = accountDao.queryForAll();
        assertEquals("Should have found same number of accounts in map", accounts.size(), all.size());
        for (Account account : all) {
            assertTrue("Should have found account in map", accounts.containsValue(account));
            verifyAccount(accounts.get(account.getName()), account);
        }

        // цикл по элементам в базе данных
        int accountC = 0;
        for (Account account : accountDao) {
            assertTrue("Should have found account in map", accounts.containsValue(account));
            verifyAccount(accounts.get(account.getName()), account);
            accountC++;
        }
        assertEquals("Should have found the right number of accounts in for loop", accounts.size(), accountC);
    }

    /**
     * Пример созданного запроса с ? аргумент с помощью объекта {@link SelectArg}. Затем вы можете установить значение
     * этот объект позже
     */
    private void useSelectArgFeature() throws Exception {

        String name1 = "foo";
        String name2 = "bar";
        String name3 = "baz";
        assertEquals(1, accountDao.create(new Account(name1)));
        assertEquals(1, accountDao.create(new Account(name2)));
        assertEquals(1, accountDao.create(new Account(name3)));

        QueryBuilder<Account, Integer> statementBuilder = accountDao.queryBuilder();
        SelectArg selectArg = new SelectArg();
        // создайте запрос с предложением WHERE, установленным в 'name =?'
        statementBuilder.where().like(Account.NAME_FIELD_NAME, selectArg);
        PreparedQuery<Account> preparedQuery = statementBuilder.prepare();

        // теперь мы можем установить аргумент выбора (?) и запустить запрос
        selectArg.setValue(name1);
        List<Account> results = accountDao.query(preparedQuery);
        assertEquals("Should have found 1 account matching our query", 1, results.size());
        assertEquals(name1, results.get(0).getName());

        selectArg.setValue(name2);
        results = accountDao.query(preparedQuery);
        assertEquals("Should have found 1 account matching our query", 1, results.size());
        assertEquals(name2, results.get(0).getName());

        selectArg.setValue(name3);
        results = accountDao.query(preparedQuery);
        assertEquals("Should have found 1 account matching our query", 1, results.size());
        assertEquals(name3, results.get(0).getName());
    }

    /**
     * Пример созданного запроса с ? аргумент с помощью объекта {@link SelectArg}. Затем вы можете установить значение
     * этот объект позднее.
     */
    private void useTransactions(ConnectionSource connectionSource) throws Exception {
        String name = "trans1";
        final Account account = new Account(name);
        assertEquals(1, accountDao.create(account));

        TransactionManager transactionManager = new TransactionManager(connectionSource);
        try {
            // попробовать что-то в транзакции
            transactionManager.callInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // мы делаем удаление
                    assertEquals(1, accountDao.delete(account));
                    assertNull(accountDao.queryForId(account.getId()));
                    // но затем (как пример) мы выбрасываем исключение, которое откатывает удаление
                    throw new Exception("We throw to roll back!!");
                }
            });
            fail("This should have thrown");
        } catch (SQLException e) {
            // expected
        }

        assertNotNull(accountDao.queryForId(account.getId()));
    }

    /**
     * Убедитесь, что учетная запись, хранящаяся в базе данных, совпадает с ожидаемым объектом.
     */
    private void verifyDb(int id, Account expected) throws SQLException, Exception {
        // убедитесь, что мы можем прочитать его обратно
        Account account2 = accountDao.queryForId(id);
        if (account2 == null) {
            throw new Exception("Should have found id '" + id + "' in the database");
        }
        verifyAccount(expected, account2);
    }

    /**
     * Убедитесь, что учетная запись совпадает с ожидаемой.
     */
    private static void verifyAccount(Account expected, Account account2) {
        assertEquals("ожидаемое имя не совпадает с именем учетной записи", expected, account2);
        assertEquals("ожидаемый пароль не совпадает с именем учетной записи", expected.getPassword(), account2.getPassword());
    }
}