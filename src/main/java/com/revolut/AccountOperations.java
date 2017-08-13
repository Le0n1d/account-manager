package com.revolut;

class AccountOperations {
    /** Adds <code>money</code> to the account. */
    static Account updateAccountWithMoney(Account account, double deltaMoney) {
        return new Account(account.getId(), account.getOwnerId(), account.getMoney() + deltaMoney);
    }
}
