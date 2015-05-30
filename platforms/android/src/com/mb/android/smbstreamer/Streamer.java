/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Credit: https://github.com/MizzleDK/Mizuu/blob/master/app/src/main/java/com/miz/smbstreamer/Streamer.java

package com.mb.android.smbstreamer;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Properties;

import mediabrowser.model.logging.ILogger;

public class Streamer extends StreamServer {

    public static final String
            HTTP_OK = "200 OK",
            HTTP_PARTIALCONTENT = "206 Partial Content",
            HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable",
            HTTP_FORBIDDEN = "403 Forbidden",
            HTTP_NOTFOUND = "404 Not Found",
            HTTP_BADREQUEST = "400 Bad Request",
            HTTP_INTERNALERROR = "500 Internal Server Error";

    public static final int PORT = 50002;
    private String mUrl = "http://127.0.0.1:" + PORT;

    private static Streamer sInstance;
    private ILogger logger;

    protected Streamer(int port, ILogger logger) throws IOException {
        super(port, new File("."));
        this.logger = logger;

        mUrl = "http://localhost:" + PORT;
    }

    public String getUrl(String path) {
        return mUrl + "?path=" + URLEncoder.encode(path);
    }

    public static Streamer getInstance(ILogger logger) {
        if (sInstance == null)
            try {
                sInstance = new Streamer(PORT, logger);
            } catch (IOException e) {
                e.printStackTrace();
            }
        return sInstance;
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties params, Properties files) {
        Response res = null;
        try {
            String sourceFile = getFilePathFromUrl(params);

            if (sourceFile == null)
                res = new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, null);
            else {
                long startFrom = 0;
                long endAt = -1;
                String range = header.getProperty("range");
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length());
                        int minus = range.indexOf('-');
                        try {
                            if (minus > 0) {
                                startFrom = Long.parseLong(range.substring(0, minus));
                                endAt = Long.parseLong(range.substring(minus + 1));
                            }
                        } catch (NumberFormatException nfe) {}
                    }
                }
                Log.d("Streamer", "Request: " + range + " from: " + startFrom + ", to: " + endAt);

                // Change return code and add Content-Range header when skipping is requested
                final StreamSource source = new StreamSource(sourceFile, logger);
                long fileLen = source.length();
                if (range != null && startFrom > 0) {
                    if (startFrom >= fileLen) {
                        res = new Response(HTTP_RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, null);
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    } else {
                        if (endAt < 0)
                            endAt = fileLen - 1;
                        long newLen = fileLen - startFrom;
                        if (newLen < 0)
                            newLen = 0;
                        Log.d("Streamer", "start=" + startFrom + ", endAt=" + endAt + ", newLen=" + newLen);
                        final long dataLen = newLen;
                        source.moveTo(startFrom);
                        Log.d("Streamer", "Skipped " + startFrom + " bytes");

                        res = new Response(HTTP_PARTIALCONTENT, source.getMimeType(), source);
                        res.addHeader("Content-length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    }
                } else {
                    source.reset();
                    res = new Response(HTTP_OK, source.getMimeType(), source);
                    res.addHeader("Content-Length", "" + fileLen);
                }
            }
        } catch (IOException ioe) {
            res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, null);
        }

        // Announce that the file server accepts partial content requestes
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    public static String getFilePathFromUrl(Properties params) {

        return params.getProperty("path");
    }
}