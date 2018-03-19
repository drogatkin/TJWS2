package com.rslakra.mobile.utils;

import android.os.Debug;

import com.rslakra.mobile.logger.LogHelper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Created by admin on 3/18/18.
 */

public final class INetHelper {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "INetHelper";
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
    public static InetAddress lookupINetAddress(boolean loopBackAddressOnly) {
        InetAddress iNetAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if(networkInterfaces != null) {
                while(networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Enumeration<InetAddress> iNetAddresses = networkInterface.getInetAddresses();
                    while(iNetAddresses.hasMoreElements()) {
                        InetAddress netAddress = iNetAddresses.nextElement();
                        if(loopBackAddressOnly) {
                            if(netAddress.isLoopbackAddress()) {
                                if(!netAddress.isSiteLocalAddress() && isIPv4Address(netAddress.getHostAddress())) {
                                    return netAddress;
                                }
                                
                                iNetAddress = netAddress;
                            }
                        } else {
                            if(!netAddress.isLoopbackAddress()) {
                                if(!netAddress.isSiteLocalAddress() && isIPv4Address(netAddress.getHostAddress())) {
                                    return netAddress;
                                }
                                
                                iNetAddress = netAddress;
                            }
                        }
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex.toString());
        }
        
        return iNetAddress;
    }
}
