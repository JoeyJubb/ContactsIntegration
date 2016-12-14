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

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joefr_000 on 14/12/2016.
 */
public class RestApi {

    private final Context context;

    public RestApi(Context context) {

        this.context = context;
    }

    public Contacts getContacts(String accountName, String authToken) {

        // dummy implementation returns a list created from assets, regardless of local numbers
        Type listType = new TypeToken<ArrayList<Contact>>() {
        }.getType();
        final GsonBuilder builder = new GsonBuilder().registerTypeAdapter(DateTime.class, new DateTimeDeserializer());
        final Gson gson = builder.create();
        final List<Contact> contactList = gson.fromJson(
                AssetReader.getStringFromAssetts(context, "contacts.json", "[]")
                , listType);

        return new Contacts(contactList);

    }
}
