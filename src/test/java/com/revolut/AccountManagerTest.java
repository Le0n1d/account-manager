package com.revolut;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.revolut.AccountManagerConstants.ACCOUNT_MANAGER;
import static com.revolut.AccountManagerConstants.PATH_DEPOSIT;
import static com.revolut.AccountManagerConstants.PATH_GET_ACCOUNT;
import static com.revolut.AccountManagerConstants.PATH_OPEN_ACCOUNT;
import static com.revolut.AccountManagerConstants.PATH_TRANSFER;
import static com.revolut.AccountManagerConstants.PATH_WITHDRAW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


public class AccountManagerTest {
    private static final double EPSILON = 1e-7;
    private static final int TEST_OWNER_ID = 123;

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        server = Main.startServer();
        Client c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void whenNewAccountIsOpenedItShouldHaveZeroMoney() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(TEST_OWNER_ID, account.getOwnerId());
        assertEquals(0.0, account.getMoney(), EPSILON);
    }

    @Test
    public void whenTwoAccountsAreOpenTheyShouldHaveDifferentIds() {
        Account firstAccount = openAccountForOwner(TEST_OWNER_ID);
        Account secondAccount = openAccountForOwner(TEST_OWNER_ID);
        assertNotSame(firstAccount.getId(), secondAccount.getId());
    }

    @Test
    public void whenMoneyDepositedShouldShouldTheSameMoneyInTheAccount() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, account.getMoney(), EPSILON);

        double money = 1000.0;
        deposit(account.getId(), money);
        Account verifyAccount = getAccount(account.getId());
        assertEquals(money, verifyAccount.getMoney(), EPSILON);
    }

    @Test
    public void whenMoneyDepositedTwiceThenBothDepositsShouldBeStoredInTheAccount() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, account.getMoney(), EPSILON);

        double firstDepositMoney = 1000.0;
        deposit(account.getId(), firstDepositMoney);
        double secondDepositMoney = 2000.0;
        deposit(account.getId(), secondDepositMoney);

        Account verifyAccount = getAccount(account.getId());
        assertEquals(firstDepositMoney + secondDepositMoney, verifyAccount.getMoney(), EPSILON);
    }

    @Test
    public void whenMoneyWithdrawnThenAccountMoneyShouldReduceByTheWithdrawnAmount() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, account.getMoney(), EPSILON);

        double depositMoney = 1000.0;
        deposit(account.getId(), depositMoney);
        Account verifyAccount = getAccount(account.getId());
        assertEquals(depositMoney, verifyAccount.getMoney(), EPSILON);

        double withdrawMoney = 200.0;
        withdraw(account.getId(), withdrawMoney);
        verifyAccount = getAccount(account.getId());
        assertEquals(800.0, verifyAccount.getMoney(), EPSILON);
    }

    @Test
    public void whenMoneyTransferredBetweenTwoAccountsTheMoneyInBothAccountsShouldBeUpdated() {
        Account firstAccount = openAccountForOwner(TEST_OWNER_ID);
        Account secondAccount = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, firstAccount.getMoney(), EPSILON);
        assertEquals(0.0, secondAccount.getMoney(), EPSILON);

        deposit(firstAccount.getId(), 100);
        deposit(secondAccount.getId(), 200);

        transfer(firstAccount.getId(), secondAccount.getId(), 20);

        Account updatedFirstAccount = getAccount(firstAccount.getId());
        Account updatedSecondAccount = getAccount(secondAccount.getId());
        assertEquals(80.0, updatedFirstAccount.getMoney(), EPSILON);
        assertEquals(220.0, updatedSecondAccount.getMoney(), EPSILON);
    }

    @Test
    public void whenMoneyTransferExceedsLimitsShouldNotAlterTheOriginalStateOfTheAccounts() {
        Account firstAccount = openAccountForOwner(TEST_OWNER_ID);
        Account secondAccount = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, firstAccount.getMoney(), EPSILON);
        assertEquals(0.0, secondAccount.getMoney(), EPSILON);

        deposit(firstAccount.getId(), 10);
        deposit(secondAccount.getId(), AccountManagerAPI.MAX_MONEY);

        boolean exceptionThrown = false;
        try {
            transfer(firstAccount.getId(), secondAccount.getId(), 10);
            /* Second account would be over the limit if transfer succeeded, so exception expected. */
        } catch (BadRequestException e) {
            exceptionThrown = true;
            Account updatedFirstAccount = getAccount(firstAccount.getId());
            Account updatedSecondAccount = getAccount(secondAccount.getId());
            assertEquals(10, updatedFirstAccount.getMoney(), EPSILON);
            assertEquals(AccountManagerAPI.MAX_MONEY, updatedSecondAccount.getMoney(), EPSILON);
        }
        assertTrue(exceptionThrown);
    }

    @Test(expected = BadRequestException.class)
    public void whenMoneyTransferRequestInTheSameAccountShouldFail() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        assertEquals(0.0, account.getMoney(), EPSILON);

        deposit(account.getId(), 100);

        transfer(account.getId(), account.getId(), 20);
    }

    @Test(expected = BadRequestException.class)
    public void whenNegativeMoneyDepositAttemptedShouldFail() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        double money = -100.0;
        deposit(account.getId(), money);
    }

    @Test(expected = BadRequestException.class)
    public void whenNegativeMoneyWithdrawalAttemptedShouldFail() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        double money = -100.0;
        withdraw(account.getId(), money);
    }

    @Test(expected = BadRequestException.class)
    public void whenNonExistentAccountRequestedShouldFail() {
        getAccount(2);
    }

    @Test(expected = BadRequestException.class)
    public void whenMaximumAccountLimitReachedShouldFail() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        double money = AccountManagerAPI.MAX_MONEY + 1;
        deposit(account.getId(), money);
    }

    @Test(expected = BadRequestException.class)
    public void whenMinimumAccountLimitReachedShouldFail() {
        Account account = openAccountForOwner(TEST_OWNER_ID);
        double money = AccountManagerAPI.MIN_MONEY - 1;
        withdraw(account.getId(), money);
    }

    private Account openAccountForOwner(long ownerId) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(AccountManagerConstants.PARAM_OWNER_ID, Long.toString(ownerId));
        javax.ws.rs.core.Response response =
                getRequest(PATH_OPEN_ACCOUNT)
                        .post(Entity.form(formData));
        validateResponse(response);
        return response.readEntity(Account.class);
    }

    private Account getAccount(long accountId) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(AccountManagerConstants.PARAM_ACCOUNT_ID, Long.toString(accountId));
        javax.ws.rs.core.Response response =
                getRequest(PATH_GET_ACCOUNT)
                        .post(Entity.form(formData));
        validateResponse(response);
        return response.readEntity(Account.class);
    }

    private void deposit(long accountId, double money) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(AccountManagerConstants.PARAM_ACCOUNT_ID, Long.toString(accountId));
        formData.add(AccountManagerConstants.PARAM_MONEY, Double.toString(money));
        javax.ws.rs.core.Response response =
                getRequest(PATH_DEPOSIT)
                        .post(Entity.form(formData));
        validateResponse(response);
    }

    private void withdraw(long accountId, double money) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(AccountManagerConstants.PARAM_ACCOUNT_ID, Long.toString(accountId));
        formData.add(AccountManagerConstants.PARAM_MONEY, Double.toString(money));
        javax.ws.rs.core.Response response =
                getRequest(PATH_WITHDRAW)
                        .post(Entity.form(formData));
        validateResponse(response);
    }

    private void transfer(long sourceAccountId, long targetAccountId, double money) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(AccountManagerConstants.PARAM_SOURCE_ACCOUNT_ID, Long.toString(sourceAccountId));
        formData.add(AccountManagerConstants.PARAM_TARGET_ACCOUNT_ID, Long.toString(targetAccountId));
        formData.add(AccountManagerConstants.PARAM_MONEY, Double.toString(money));
        javax.ws.rs.core.Response response =
                getRequest(PATH_TRANSFER)
                        .post(Entity.form(formData));
        validateResponse(response);
    }

    private void validateResponse(Response response) {
        if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            throw new BadRequestException(response.readEntity(String.class));
        }
    }

    private Invocation.Builder getRequest(String path) {
        return target.path(ACCOUNT_MANAGER + path).request(MediaType.APPLICATION_JSON_TYPE);
    }
}
