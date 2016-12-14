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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_DIALOG = "state_dialog";
    private static final String STATE_CURRENT_ACTION = "state_invalidate";

    private static final int REQ_PERMISSION_AUTO = 3;
    private static final int REQ_PERMISSION_MANUAL = 4;

    private String TAG = this.getClass().getSimpleName();
    private AccountManager mAccountManager;
    private AlertDialog mAlertDialog;
    private AccountAction mPendingAccountAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mAccountManager = AccountManager.get(this);

        findViewById(R.id.btnAddAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewAccount(Constants.ACCOUNT_TYPE, Constants.AUTH_TOKEN_TYPE);
            }
        });

        findViewById(R.id.btnGetAuthToken).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPicker(Constants.AUTH_TOKEN_TYPE, AccountAction.SYNC);
            }
        });
        findViewById(R.id.btnInvalidateAuthToken).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPicker(Constants.AUTH_TOKEN_TYPE, AccountAction.REMOVE);
            }
        });

        if (savedInstanceState != null) {
            boolean showDialog = savedInstanceState.getBoolean(STATE_DIALOG);
            AccountAction invalidate = AccountAction.valueOf(savedInstanceState.getString(STATE_CURRENT_ACTION));
            if (showDialog) {
                showAccountPicker(Constants.AUTH_TOKEN_TYPE, invalidate);
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            outState.putBoolean(STATE_DIALOG, true);
            outState.putString(STATE_CURRENT_ACTION, mPendingAccountAction.name());
        }
    }

    /**
     * Add new account to the account manager
     *
     * @param accountType
     * @param authTokenType
     */
    private void addNewAccount(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    showMessage("Account was created");
                    Log.d("udinic", "AddNewAccount Bundle is " + bnd);

                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQ_PERMISSION_AUTO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAccountPicker(Constants.AUTH_TOKEN_TYPE, mPendingAccountAction);
            } else {
                showPermissionRequestSnackbar();
            }
        } else if (requestCode == REQ_PERMISSION_MANUAL) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAccountPicker(Constants.AUTH_TOKEN_TYPE, mPendingAccountAction);
            } else {
                showMessage("Cannot show accounts without permission");
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Show all the accounts registered on the account manager. Request an auth token upon user select.
     *
     * @param authTokenType
     */
    private void showAccountPicker(final String authTokenType, final AccountAction accountAction) {
        mPendingAccountAction = accountAction;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.GET_ACCOUNTS)) {
                showPermissionRequestSnackbar();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQ_PERMISSION_AUTO);
            }
            return;
        }

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        if (availableAccounts.length == 0) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show();
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            mAlertDialog = new AlertDialog.Builder(this).setTitle("Pick Account").setAdapter(new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, name), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (accountAction) {

                        case REMOVE:
                            removeAccount(availableAccounts[which]);
                            break;
                        case SYNC:
                            syncAccount(availableAccounts[which]);
                            break;
                    }
                }

            }).create();
            mAlertDialog.show();
        }
    }

    private void syncAccount(Account account) {


        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(account, ContactsContract.AUTHORITY, settingsBundle);


        showMessage("Sync requested, view results in Contacts app");

    }

    private void removeAccount(Account account) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mAccountManager.removeAccount(account, this, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {

                    try {
                        Boolean result = future.getResult().getBoolean(AccountManager.KEY_BOOLEAN_RESULT);
                        if (result) {
                            showMessage("Account deleted");
                        } else {
                            showMessage("Account not deleted");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showMessage(e.getMessage());
                    }
                }
            }, new Handler());
        } else {
            mAccountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        Boolean result = future.getResult();
                        if (result) {
                            showMessage("Account deleted");
                        } else {
                            showMessage("Account not deleted");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showMessage(e.getMessage());
                    }

                }
            }, new Handler());
        }
    }

    private void showPermissionRequestSnackbar() {
        Snackbar.make(findViewById(R.id.btnGetAuthToken), R.string.permission_rationale_accounts, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    @TargetApi(Build.VERSION_CODES.M)
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQ_PERMISSION_MANUAL);
                    }
                });
    }

    private void showMessage(final String msg) {
        if (TextUtils.isEmpty(msg))
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private enum AccountAction {

        REMOVE, SYNC

    }
}