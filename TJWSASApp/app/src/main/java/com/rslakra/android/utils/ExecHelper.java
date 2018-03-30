package com.rslakra.android.utils;

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
