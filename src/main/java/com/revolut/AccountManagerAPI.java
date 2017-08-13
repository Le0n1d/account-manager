package com.revolut;

import javax.inject.Singleton;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.revolut.AccountManagerConstants.ACCOUNT_MANAGER;
import static com.revolut.AccountManagerConstants.PARAM_ACCOUNT_ID;
import static com.revolut.AccountManagerConstants.PARAM_MONEY;
import static com.revolut.AccountManagerConstants.PARAM_OWNER_ID;
import static com.revolut.AccountManagerConstants.PARAM_SOURCE_ACCOUNT_ID;
import static com.revolut.AccountManagerConstants.PARAM_TARGET_ACCOUNT_ID;
import static com.revolut.AccountManagerConstants.PATH_DEPOSIT;
import static com.revolut.AccountManagerConstants.PATH_GET_ACCOUNT;
import static com.revolut.AccountManagerConstants.PATH_OPEN_ACCOUNT;
import static com.revolut.AccountManagerConstants.PATH_TRANSFER;
import static com.revolut.AccountManagerConstants.PATH_WITHDRAW;

/**
 * Root resource exposed at {@link AccountManagerConstants#ACCOUNT_MANAGER}.
 * <p\>
 * See {@link AccountManager} for an overview.
 */
@Path(ACCOUNT_MANAGER)
@Singleton // To keep the state of the in-memory storage between the calls to the API.
public class AccountManagerAPI {
    final static double MIN_MONEY = 0;
    final static double MAX_MONEY = 1e6;

    private final AccountManager accountManager;

    public AccountManagerAPI() {
        this.accountManager = new AccountManagerWithMoneyLimits(MIN_MONEY, MAX_MONEY);
    }

    @POST
    @Path(PATH_OPEN_ACCOUNT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response openAccount(@FormParam(PARAM_OWNER_ID) long ownerId) {
        Account account = accountManager.openAccount(ownerId);
        return Response.status(Response.Status.OK).entity(account).build();
    }

    @POST
    @Path(PATH_GET_ACCOUNT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccount(@FormParam(PARAM_ACCOUNT_ID) long accountId) {
        try {
            Account account = accountManager.getAccount(accountId);
            return Response.status(Response.Status.OK).entity(account).build();
        } catch (AccountManager.AccountOperationException e) {
            return createBadRequestMessage(e);
        }
    }

    @POST
    @Path(PATH_DEPOSIT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deposit(@FormParam(PARAM_ACCOUNT_ID) long accountId,
                            @FormParam(PARAM_MONEY) double money) {
        try {
            accountManager.deposit(accountId, money);
            return Response.status(Response.Status.OK).build();
        } catch (AccountManager.AccountOperationException e) {
            return createBadRequestMessage(e);
        }
    }

    @POST
    @Path(PATH_WITHDRAW)
    @Produces(MediaType.APPLICATION_JSON)
    public Response withdraw(@FormParam(PARAM_ACCOUNT_ID) long accountId,
                             @FormParam(PARAM_MONEY) double money) {
        /* While this API call is very similar to <code>deposit</code>, semantically, they are likely to take different
           evolution paths (e.g. different error handling scenarios, etc), so it was decided to maintain the two entry
           points separately. */
        try {
            accountManager.withdraw(accountId, money);
            return Response.status(Response.Status.OK).build();
        } catch (AccountManager.AccountOperationException e) {
            return createBadRequestMessage(e);
        }
    }

    @POST
    @Path(PATH_TRANSFER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transfer(@FormParam(PARAM_SOURCE_ACCOUNT_ID) long sourceAccountId,
                             @FormParam(PARAM_TARGET_ACCOUNT_ID) long targetAccountId,
                             @FormParam(PARAM_MONEY) double money) {
        try {
            accountManager.transfer(sourceAccountId, targetAccountId, money);
            return Response.status(Response.Status.OK).build();
        } catch (AccountManager.AccountOperationException e) {
            return createBadRequestMessage(e);
        }
    }

    private Response createBadRequestMessage(AccountManager.AccountOperationException e) {
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
}
