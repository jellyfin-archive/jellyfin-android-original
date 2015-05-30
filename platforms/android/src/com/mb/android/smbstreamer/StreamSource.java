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

// Credit: https://github.com/MizzleDK/Mizuu/blob/master/app/src/main/java/com/miz/smbstreamer/StreamSource.java

package com.mb.android.smbstreamer;
import com.mb.android.io.NativeFileSystem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import mediabrowser.model.logging.ILogger;
import mediabrowser.model.net.MimeTypes;

public class StreamSource {

    protected String mime, name;
    protected long fp, len;
    protected int bufferSize;
    protected InputStream input;
    protected String path;
    private ILogger logger;

    public StreamSource(String path, ILogger logger) throws IOException {
        this.logger = logger;
        fp = 0;
        mime = MimeTypes.GetMimeType(path);
        bufferSize = 16 * 1024;
        this.path = path;
        setValues(path);
    }

    public void open() throws IOException {
        try {
            input  = getInputStream(path);

            if (fp > 0)
                input.skip(fp);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void setValues(String path) throws IOException {

        NativeFileSystem fs = new NativeFileSystem(logger);

        name = fs.getFileName(path);
        len = fs.getFileLength(path);
    }

    private InputStream getInputStream(String path) throws IOException {

        NativeFileSystem fs = new NativeFileSystem(logger);

        InputStream fileStream = fs.getFileInputStream(path);

        return new BufferedInputStream(fileStream, bufferSize);
    }

    public int read(byte[] buff) throws IOException{
        return read(buff, 0, buff.length);
    }

    public int read(byte[] bytes, int start, int offs) throws IOException {
        int read =  input.read(bytes, start, offs);
        fp += read;
        return read;
    }

    public long moveTo(long position) throws IOException {
        fp = position;
        return fp;
    }

    public void close() {
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMimeType(){
        return mime;
    }

    public long length(){
        return len;
    }

    public String getName(){
        return name;
    }

    public long available(){
        return len - fp;
    }

    public void reset(){
        fp = 0;
    }

    public int getBufferSize(){
        return bufferSize;
    }
}
