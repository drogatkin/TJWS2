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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM
 */
public final class NetHelper {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "NetHelper";
    /**
     * APACHE InetAddressUtils is deprecated in API level 22
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
    private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");
    
    /**
     * @param input
     * @return
     */
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
    
    /**
     * @param input
     * @return
     */
    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }
    
    /**
     * @param input
     * @return
     */
    public static boolean isIPv6HexCompressedAddress(final String input) {
        return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }
    
    /**
     * @param input
     * @return
     */
    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }
    
    /**
     * Returns the loopback address, if the <code>loopBackAddressOnly</code> it
     * set to true otherwise returns non-loopback address.
     *
     * @return
     */
    public static InetAddress lookupINetAddress(boolean onlyLoopBackAddress) {
        InetAddress resultAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if(networkInterfaces != null) {
                while(networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Enumeration<InetAddress> iNetAddresses = networkInterface.getInetAddresses();
                    while(iNetAddresses.hasMoreElements()) {
                        InetAddress netAddress = iNetAddresses.nextElement();
                        if(onlyLoopBackAddress) {
                            if(netAddress.isLoopbackAddress()) {
                                if(isIPv4Address(netAddress.getHostAddress())) {
                                    return netAddress;
                                }
                                
                                resultAddress = netAddress;
                            }
                        } else {
                            if(!netAddress.isLoopbackAddress()) {
                                if(!netAddress.isSiteLocalAddress() && isIPv4Address(netAddress.getHostAddress())) {
                                    return netAddress;
                                }
                                
                                resultAddress = netAddress;
                            }
                        }
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex.toString());
        }
        
        return resultAddress;
    }
    
    
    /**
     * @return
     */
    public static InetAddress getLookBackAddress() {
        InetAddress iNetAddress = null;
        try {
            for(Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                for(Enumeration<InetAddress> itrAddress = networkInterface.getInetAddresses(); itrAddress.hasMoreElements(); ) {
                    InetAddress inetAddress = itrAddress.nextElement();
                    if(inetAddress.isLoopbackAddress()) {
                        if(isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                        
                        iNetAddress = inetAddress;
                        LogHelper.d(LOG_TAG, "processed address:" + iNetAddress);
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return iNetAddress;
    }
    
    /**
     * @param onlyLoopBackAddress
     * @return
     */
    public static InetAddress getNonLookupAddress(boolean onlyLoopBackAddress) {
        InetAddress result = null;
        try {
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress()) {
                        if((!inetAddress.isSiteLocalAddress() || onlyLoopBackAddress) && isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                        result = inetAddress;
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return result;
    }
    
}
