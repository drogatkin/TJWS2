// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
package com.rslakra.android.framework;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.Socket;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/26 12:59 PM
 */
public final class ExecHelper {
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "ExecHelper";
    
    /** Singleton object */
    private ExecHelper() {
        throw new UnsupportedOperationException("Object creation not allowed for this class.");
    }
    
    /**
     * Closes all the objects safely.
     *
     * Note: - On Android API levels prior to 19, <code>Socket</code> class does not implement
     * <code>Closeable</code> interface.
     *
     * @param objects
     */
    public static final void closeSafely(final Object... objects) {
        for(Object object : objects) {
            if(object != null) {
                try {
                    Log.d(LOG_TAG, "Closing:" + object);
                    if(object instanceof Closeable) {
                        ((Closeable) object).close();
                    } else if(object instanceof Socket) {
                        ((Socket) object).close();
                    } else if(object instanceof DatagramSocket) {
                        ((DatagramSocket) object).close();
                    } else {
                        final String message = "Unable to close:" + object;
                        Log.d(LOG_TAG, message);
                        throw new RuntimeException(message);
                    }
                } catch(Throwable ex) {
                    Log.e(LOG_TAG, Log.getStackTraceString(ex));
                }
            }
        }
    }
    
    /**
     * Reads the <code>mInputStream</code> and return the response as string.
     *
     * @param mInputStream
     * @return
     * @throws IOException
     */
    public static final String readFully(final InputStream mInputStream) throws IOException {
        String result = null;
        ByteArrayOutputStream mOutputStream = null;
        try {
            mOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while((length = mInputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, length);
            }
            result = mOutputStream.toString("UTF-8");
        } catch(IOException ex) {
            Log.e(LOG_TAG, Log.getStackTraceString(ex));
            throw ex;
        } finally {
            closeSafely(mOutputStream);
        }
        
        return result;
    }
    
    /**
     * Runs the commands on the application terminal (you can find it in google play).
     *
     * @param commands
     * @return
     */
    public static final String execCommands(final String... commands) {
        String result = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try {
            final Process mProcess = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(mProcess.getOutputStream());
            response = mProcess.getInputStream();
            
            for(final String string : commands) {
                outputStream.writeBytes(string + "\n");
                outputStream.flush();
            }
            
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                mProcess.waitFor();
            } catch(InterruptedException ex) {
            }
            result = readFully(response);
        } catch(IOException ex) {
            Log.e(LOG_TAG, Log.getStackTraceString(ex));
        } finally {
            closeSafely(outputStream, response);
        }
        
        return result;
    }
}
