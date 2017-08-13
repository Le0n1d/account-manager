package com.revolut;

/**
 * A simple account manager with the following operations
 * <ul>
 * <li>account creation and retrieval</li>
 * <li>money deposit</li>
 * <li>money withdrawal</li>
 * <li>money transfer between two accounts</li>
 * </ul>
 */
public interface AccountManager {
    /**
     * Opens an empty account for the specified <code>ownerId</code>.
     */
    Account openAccount(long ownerId);

    /**
     * Returns {@link Account} details for a specified <code>accountId</code>.
     *
     * @throws AccountOperationException if account doesn't exist.
     */
    Account getAccount(long accountId) throws AccountOperationException;

    /**
     * Attempts to deposit the specified amount of positive <code>money</code> into the account with <code>accountId</code>.
     *
     * @throws AccountOperationException if account limits are exceeded (e.g. insufficient funds).
     */
    void deposit(long accountId, double money) throws AccountOperationException;

    /**
     * Attempts to withdraw the specified amount of positive <code>money</code> from the account with <code>accountId</code>.
     *
     * @throws AccountOperationException if account limits are exceeded (e.g. insufficient funds).
     */
    void withdraw(long accountId, double money) throws AccountOperationException;

    /**
     * Attempts to transfer specified amount of <code>money</code> from <code>sourceAccountId</code>
     * to <code>targetAccountId</code>.
     *
     * @param sourceAccountId the source account to transfer the <code>money</code> from.
     * @param targetAccountId the target account to transfer the <code>money</code> to.
     * @throws AccountOperationException if accounts limits are exceeded (e.g. insufficient funds), or accounts are
     *                                   the same.
     */
    void transfer(long sourceAccountId, long targetAccountId, double money) throws AccountOperationException;

    /**
     * Account operation exception (e.g. insufficient funds).
     */
    class AccountOperationException extends Exception {
        AccountOperationException(String message) {
            super(message);
        }
    }
}
