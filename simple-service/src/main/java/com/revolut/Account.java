package com.revolut;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

/**
 * A bean to hold account definition.
 */
public class Account {
    private final long id;
    private final long ownerId;
    private final double money;

    /**
     * @param id      A unique account identifier.
     * @param ownerId Owner of the account.
     * @param money   Amount of signed money in the account.
     */
    @JsonbCreator
    public Account(@JsonbProperty("id") long id,
                   @JsonbProperty("ownerId") long ownerId,
                   @JsonbProperty("money") double money) {
        this.id = id;
        this.ownerId = ownerId;
        this.money = money;
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public double getMoney() {
        return money;
    }
}
