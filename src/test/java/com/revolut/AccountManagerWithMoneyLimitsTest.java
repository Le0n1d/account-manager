package com.revolut;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class AccountManagerWithMoneyLimitsTest {
    private AtomicLong maxAccountId = new AtomicLong(0);
    private AccountManager accountManager;

    private static final long MAX_READS = 3000000;
    private static final long MAX_WRITES = 3000000;

    @Test
    public void getAccount() throws Exception {
        accountManager = new AccountManagerWithMoneyLimits(0, 1000);
        accountManager.openAccount(1);

        Thread writeThread = new Thread(new WriteAccount());
        writeThread.start();

        Thread readThread = new Thread(new ReadAccount());
        readThread.start();

        writeThread.join();
        readThread.join();
    }

    private class WriteAccount implements Runnable {
        @Override
        public void run() {
            System.out.println("Started Writing...");
            for (int i = 0; i < MAX_WRITES; i++) {
                try {
                    Account account = accountManager.openAccount(1);
                    maxAccountId.set(account.getId());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            System.out.println("Finished Writing.");
        }
    }

    private class ReadAccount implements Runnable {
        @Override
        public void run() {
            System.out.println("Started Reading...");
            Random random = new Random();
            long accountId;
            for (int i = 0; i < MAX_READS; i++) {
                accountId = Math.abs(random.nextLong()) % maxAccountId.get();
                try {
                    Account account = accountManager.getAccount(accountId);
                    assertEquals(accountId, account.getId());
                } catch (Exception e) {
                    System.out.println("Unable to retrieve account " + accountId);
                    throw new IllegalStateException(e);
                }
            }
            System.out.println("Finished Reading...");
        }
    }
}