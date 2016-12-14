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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by joefr_000 on 14/12/2016.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";


    public SyncAdapter(Context context) {
        super(context, true);
    }

    private static void addContact(Context conext, ContentProviderClient contentResolver, Account account, Contact contact) {
        Log.i(TAG, "Adding contact: " + contact.getDisplayName());
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        //Create our RawContact
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        builder.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type);
        builder.withValue(ContactsContract.RawContacts.SYNC1, contact.getDisplayName());
        builder.withValue(ContactsContract.RawContacts.SOURCE_ID, contact.getSourceId());
        operationList.add(builder.build());

        //Create a Data record of common type 'StructuredName' for our RawContact
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getDisplayName());
        builder.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.getFirstName());
        builder.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.getLastName());
        operationList.add(builder.build());

        //Create a Data record of common type "Phone" for our RawContact
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getPhone());
        operationList.add(builder.build());

        //Create a Data record of custom type "vnd.android.cursor.item/vnd.fm.last.android.profile" to display a link to the ContactsIntegration profile
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, Constants.CONTACT_MIME_TYPE);
        builder.withValue(ContactsContract.Data.DATA1, contact.getDisplayName());
        builder.withValue(ContactsContract.Data.DATA2, "Example Profile");
        builder.withValue(ContactsContract.Data.DATA3, "View profile");
        operationList.add(builder.build());

        //Create a Data record of common type "Photo" for our RawContact
        if (!TextUtils.isEmpty(contact.getImage())) {
            Bitmap myBitmap = null;
            try {
                myBitmap = Glide.with(conext)
                        .load(contact.getImage())
                        .asBitmap()
                        .centerCrop()
                        .into(500, 500)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            if (myBitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
                builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, baos.toByteArray());
                operationList.add(builder.build());
            }

        }

        try {
            contentResolver.applyBatch(operationList);
        } catch (Exception e) {
            Log.e(TAG, "Something went wrong during creation! " + e);
            e.printStackTrace();
        }
    }

    private static void updateContactStatus(ContentProviderClient contentResolver, ArrayList<ContentProviderOperation> operationList, long rawContactId, SocialNetworkStatus socialNetworkStatus) {
        Uri rawContactUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
        Uri entityUri = Uri.withAppendedPath(rawContactUri, ContactsContract.RawContacts.Entity.CONTENT_DIRECTORY);
        Cursor c = null;
        try {
            c = contentResolver.query(entityUri, new String[]{
                    ContactsContract.RawContacts.SOURCE_ID,
                    ContactsContract.RawContacts.Entity.DATA_ID,
                    ContactsContract.RawContacts.Entity.MIMETYPE,
                    ContactsContract.RawContacts.Entity.DATA1}, null, null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        if (!c.isNull(1)) {
                            String mimeType = c.getString(2);
                            String status = socialNetworkStatus.getLabel();

                            if (mimeType.equals(Constants.CONTACT_MIME_TYPE)) {
                                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.StatusUpdates.CONTENT_URI);
                                builder.withValue(ContactsContract.StatusUpdates.DATA_ID, c.getLong(1));
                                builder.withValue(ContactsContract.StatusUpdates.STATUS, status);
                                builder.withValue(ContactsContract.StatusUpdates.STATUS_RES_PACKAGE, "uk.co.bubblebearapps.contactsintegration");
                                builder.withValue(ContactsContract.StatusUpdates.STATUS_LABEL, R.string.app_name);
                                builder.withValue(ContactsContract.StatusUpdates.STATUS_ICON, R.drawable.ic_android_black_24dp);
                                builder.withValue(ContactsContract.StatusUpdates.STATUS_TIMESTAMP, socialNetworkStatus.getTimestampMillis());
                                operationList.add(builder.build());

                                builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
                                builder.withSelection(BaseColumns._ID + " = '" + c.getLong(1) + "'", null);
                                builder.withValue(ContactsContract.Data.DATA3, status);
                                operationList.add(builder.build());
                            }
                        }
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.i(TAG, "performSync: " + account.toString());
        HashMap<String, Long> localContacts = getLocalContacts(account, provider);


        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        RestApi server = Injection.getRestApi(getContext());
        String authToken = null;
        try {
            authToken = AccountManager.get(getContext()).blockingGetAuthToken(account, Constants.AUTH_TOKEN_TYPE, true);
        } catch (OperationCanceledException | IOException | AuthenticatorException e) {
            e.printStackTrace();
        }

        if (authToken == null || TextUtils.isEmpty(authToken)) {
            //TODO set error on syncResult
            return;
        }

        try {
            Contacts contacts = server.getContacts(account.name, authToken);
            for (Contact contact : contacts.getContacts()) {
                if (!localContacts.containsKey(contact.getDisplayName())) {
                    if (contact.getRealName().length() > 0)
                        addContact(getContext(), provider, account, contact);
                    else
                        addContact(getContext(), provider, account, contact);
                } else {
                    if (contact.getStatus() != null) {
                        updateContactStatus(provider, operationList, localContacts.get(contact.getDisplayName()), contact.getStatus());
                    }
                }
            }
            if (operationList.size() > 0)
                provider.applyBatch(operationList);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    private HashMap<String, Long> getLocalContacts(Account account, ContentProviderClient provider) {


        HashMap<String, Long> resultArray = new HashMap<>();

        // Load the local Last.fm contacts
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(
                ContactsContract.RawContacts.ACCOUNT_TYPE, account.type).build();
        Cursor localContactsCursor = null;
        try {
            localContactsCursor = provider.query(rawContactUri, new String[]{BaseColumns._ID, ContactsContract.RawContacts.SYNC1}, null, null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (localContactsCursor != null) {
            if (localContactsCursor.moveToFirst()) {
                do {
                    resultArray.put(localContactsCursor.getString(1), localContactsCursor.getLong(0));
                } while (localContactsCursor.moveToNext());
            }
            localContactsCursor.close();
        }

        return resultArray;
    }
}
