package com.revolut;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link AccountManager} that supports all required operations.
 * Complexity of all operations is amortized O(1), implementation is backed by the {@link HashMap} to store
 * accounts.
 * <p/>
 * Accounts can have between <code>minMoney</code> and <code>maxMoney</code>, inclusive.
 * <p/>
 * The class is not thread-safe.
 */
public class AccountManagerWithMoneyLimits implements AccountManager {
    private static final String MESSAGE_ACCOUNT_DOES_NOT_EXIST =
            "Account does not exist.";
    private static final String MESSAGE_UNABLE_TO_DEPOSIT_DUE_TO_ACCOUNT_LIMIT =
            "Unable to perform the operation due to account limits.";
    private static final String MESSAGE_MONEY_MUST_BE_POSITIVE =
            "Specified money must be positive.";
    private static final String MESSAGE_SOURCE_AND_TARGET_ACCOUNTS_MUST_BE_DIFFERENT =
            "Source and target accounts must be different.";

    private final double minMoney;
    private final double maxMoney;

    /** Maps from account id to the {@link Account}. */
    private final Map<Long, Account> accountsMap = Maps.newHashMap();

    private long maxAccountId = 0;

    /**
     * @param minMoney The minimum amount of money to allow in the account (must be zero or less).
     * @param maxMoney The maximum amount of money to allow in the account (must be positive).
     */
    AccountManagerWithMoneyLimits(double minMoney, double maxMoney) {
        Preconditions.checkState(minMoney <= 0.0);
        Preconditions.checkState(maxMoney > 0.0);
        this.minMoney = minMoney;
        this.maxMoney = maxMoney;
    }

    /** {@inheritDoc} */
    @Override
    public Account openAccount(long ownerId) {
        Account account = new Account(maxAccountId++, ownerId, 0.0);
        accountsMap.put(account.getId(), account);
        return account;
    }

    /** {@inheritDoc} */
    @Override
    public Account getAccount(long accountId) throws AccountOperationException {
        checkAccountExists(accountId);
        return accountsMap.get(accountId);
    }

    /** {@inheritDoc} */
    @Override
    public void deposit(long accountId, double money) throws AccountOperationException {
        checkPositiveMoney(money);
        checkCanUpdateMoney(accountId, money);
        updateMoney(accountId, money);
    }

    /** {@inheritDoc} */
    @Override
    public void withdraw(long accountId, double money) throws AccountOperationException {
        checkPositiveMoney(money);
        checkCanUpdateMoney(accountId, -money);
        updateMoney(accountId, -money);
    }

    /** {@inheritDoc} */
    @Override
    public void transfer(long sourceAccountId, long targetAccountId, double money) throws AccountOperationException {
        if (sourceAccountId == targetAccountId) {
            throw new AccountOperationException(MESSAGE_SOURCE_AND_TARGET_ACCOUNTS_MUST_BE_DIFFERENT);
        } else {
            checkCanUpdateMoney(sourceAccountId, -money);
            checkCanUpdateMoney(targetAccountId, money);

            updateMoney(sourceAccountId, -money);
            updateMoney(targetAccountId, money);
        }
    }

    private void checkPositiveMoney(double money) throws AccountOperationException {
        if (money <= 0.0) {
            throw new AccountOperationException(MESSAGE_MONEY_MUST_BE_POSITIVE);
        }
    }

    /**
     * Attempts to update <code>accountId</code> with signed <code>moneyDelta</code>.
     */
    private void updateMoney(long accountId, double moneyDelta) throws AccountOperationException {
        accountsMap.put(
                accountId,
                AccountOperations.updateAccountWithMoney(
                        accountsMap.get(accountId),
                        moneyDelta));
    }

    /**
     * Checks if the account can be updated with the specified <code>moneyDelta</code>.
     *
     * @throws AccountOperationException if account can't be updated.
     */
    private void checkCanUpdateMoney(long accountId, double moneyDelta) throws AccountOperationException {
        Account account = getAccount(accountId);
        double newMoney = moneyDelta + account.getMoney();
        if (newMoney < minMoney || newMoney > maxMoney) {
            throw new AccountOperationException(MESSAGE_UNABLE_TO_DEPOSIT_DUE_TO_ACCOUNT_LIMIT);
        }
    }

    private void checkAccountExists(long accountId) throws AccountOperationException {
        if (!accountsMap.containsKey(accountId)) {
            throw new AccountOperationException(MESSAGE_ACCOUNT_DOES_NOT_EXIST);
        }
    }
}
