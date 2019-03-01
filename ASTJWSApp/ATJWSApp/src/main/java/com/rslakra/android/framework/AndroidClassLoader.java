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

import com.rslakra.android.logger.LogHelper;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import dalvik.system.DexClassLoader;

/**
 * This class loader loads the classes for the webs server. Default java class
 * loader will not work in Android, so added this custom class loader as per the
 * Android specification.
 *
 * @author Rohtash Singh
 * @version 1.0.0
 * @since Apr 28, 2015 7:16:21 PM
 */
public class AndroidClassLoader extends DexClassLoader {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "AndroidClassLoader";
    
    /**
     * PATH_SEPARATOR
     */
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");
    
    /**
     * DEX_DIR_PATH
     */
    private static final String DEX_DIR_PATH = "META-INF/DEX/TJWS";
    
    /**
     * parentClassLoader
     */
    private ClassLoader parentClassLoader;
    
    /**
     * @param classPath
     * @param parentClassLoader
     */
    public AndroidClassLoader(URL[] classPath, ClassLoader parentClassLoader) {
        super(convertFilePaths(classPath), assureClassStorage(classPath), null, parentClassLoader);
        this.parentClassLoader = parentClassLoader;
        LogHelper.d(LOG_TAG, "AndroidClassLoader()");
    }
    
    /**
     * @param urls
     * @return
     */
    private static String convertFilePaths(URL[] urls) {
        StringBuilder filePaths = new StringBuilder("");
        for(URL url : urls) {
            filePaths.append(url.getPath());
            filePaths.append(PATH_SEPARATOR);
        }
        
        LogHelper.d(LOG_TAG, "Class path:" + filePaths.toString());
        return filePaths.toString();
    }
    
    /**
     * @param classPath
     * @return
     */
    private static String assureClassStorage(URL[] classPath) {
        for(URL url : classPath) {
            String file = url.getFile();
            int webINFIndex = file.indexOf("WEB-INF");
            if(webINFIndex >= 0) {
                File dexDirFile = new File(file.substring(0, webINFIndex) + DEX_DIR_PATH.intern());
                LogHelper.d(LOG_TAG, "Dex dir:" + dexDirFile + "  " + file);
                if(!dexDirFile.exists()) {
                    boolean dirCreated = dexDirFile.mkdirs();
                    if(!dirCreated) {
                        LogHelper.w(LOG_TAG, "Unable to create dir:" + dexDirFile);
                    }
                }
                
                if(dexDirFile.exists() && dexDirFile.isDirectory()) {
                    return dexDirFile.getPath();
                }
            }
        }
        
        throw new RuntimeException("Can't create dex temporary storage of " + Arrays.toString(classPath));
    }
    
    /**
     * @see dalvik.system.BaseDexClassLoader#findResource(java.lang.String)
     */
    @Override
    protected URL findResource(String name) {
        LogHelper.d(LOG_TAG, "Request for resource:" + name);
        try {
            return super.findResource(name);
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, "Error loading resource with name:" + name);
        }
        
        return parentClassLoader.getResource(name);
    }
}
