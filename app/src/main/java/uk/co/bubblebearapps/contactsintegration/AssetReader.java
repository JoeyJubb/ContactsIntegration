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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class to read text files from the assets folder
 */
public class AssetReader {

    private AssetReader() {
    }


    /**
     * Convenience method for @link {@link #getStringFromAssetts(Context, String)} that catches the exception and returns
     * the default value
     */
    public static String getStringFromAssetts(Context context, String assetPath, String defaultValue) {
        try {
            return getStringFromAssetts(context, assetPath);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * Reads a string from a text file in the assets directory
     *
     * @param assetPath relative path of file from the assets folder
     * @return a string representing the contents of the file
     * @throws IOException if the file cannot be read
     */
    public static String getStringFromAssetts(Context context, String assetPath) throws IOException {

        StringBuilder buf = new StringBuilder();
        InputStream json = context.getAssets().open(assetPath);
        BufferedReader in =
                new BufferedReader(new InputStreamReader(json, "UTF-8"));

        String str;
        while ((str = in.readLine()) != null) {
            buf.append(str);
        }

        in.close();
        return buf.toString();
    }


}
