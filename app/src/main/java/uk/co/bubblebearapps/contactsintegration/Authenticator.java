/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Bubblebear Apps Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.co.bubblebearapps.contactsintegration;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 */
public class Authenticator extends AbstractAccountAuthenticator {
    private Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    /*
     *  The user has requested to add a new account to the system.  We return an intent that will launch our login screen if the user has not logged in yet,
     *  otherwise our activity will just pass the user's credentials on to the account manager.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        Bundle reply = new Bundle();

        Intent i = new Intent(mContext, LoginActivity.class);
        i.setAction("fm.last.android.sync.LOGIN");
        i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        reply.putParcelable(AccountManager.KEY_INTENT, i);

        return reply;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        if (!Constants.AUTH_TOKEN_TYPE.equals(authTokenType)) {

            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Cannot authenticate this token type");
            return result;
        }

        // Check if we already have a cached token to return
        final AccountManager am = AccountManager.get(mContext);
        String cachedAuthToken = am.peekAuthToken(account, authTokenType);
        if (cachedAuthToken != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, cachedAuthToken);
            return result;
        } else {
            // do some server look up

            return getAuthTokenFromCloud(am, account);
        }

    }

    /*
    Round trip to rest api here for an authentication token for this account
     */
    @NonNull
    private Bundle getAuthTokenFromCloud(AccountManager am, Account account) {

        String password = am.getPassword(account);

        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        result.putString(AccountManager.KEY_AUTHTOKEN, "this_is_a_fake_token");
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        return null;
    }
}