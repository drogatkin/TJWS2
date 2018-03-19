/**
 * Copyright 2011 Dmitriy Rogatkin, All rights reserved.
 * $Id: SettingServlet.java,v 1.10 2012/04/03 06:13:58 dmitriy Exp $
 */
package com.rslakra.mobile.atjews;

import com.rslakra.mobile.server.TJWSServer;
import com.rslakra.mobile.server.TJWSService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Acme.Utils;
import Acme.Serve.Serve;

/**
 * manages Atjeews settings
 *
 * @author drogatki
 */
public class SettingServlet extends HttpServlet {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "SettingServlet";
    
    private static final String SET_PASSWORD = "*******";
    private TJWSService service;
    
    /**
     * @param service
     */
    public SettingServlet(TJWSService service) {
        this.service = service;
    }
    
    @Override
    public String getServletInfo() {
        return Serve.Identification.serverName + " "
                + Serve.Identification.serverVersion;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache");
        resp.setDateHeader("Expires", 0);
        PrintWriter pw = resp.getWriter();
        printHtmlHead(pw);
        pw.print("<form name=\"settings\" method=\"POST\" action=\"settings\" autocomplete=\"off\">");
        pw.print("<table ><tr><td>ROOT(/) web app</td><td><select name=\"root_app\" size=\"1\">");
        pw.print("<option value=\"-\">none</option>");
        pw.print("<option value=\"/\"");
        if("/".equals(service.getConfig().rootApp))
            pw.print(" selected");
        pw.print(">/ (File servlet)</option>");
        for(String servletName : service.getServletsList()) {
            if("/".equals(servletName))
                continue;
            pw.print("<option");
            if(servletName.equals(service.getConfig().rootApp))
                pw.print(" selected");
            pw.print(">");
            pw.print(Utils.htmlEncode(servletName, true));
            pw.print("</option>");
        }
        pw.print("</select></td></tr>");
        pw.print("<tr><td>Serviced folder</td><td><input type=\"text\" name=\"webroot\" value=\"");
        if(service.getConfig().wwwFolder != null)
            pw.print(Utils.htmlEncode(service.getConfig().wwwFolder, false));
        pw.print("\"></td></tr><tr><td>Virt host</td><td><input type=\"checkbox\" name=\"virt_host\" value=\"true\"");
        if(service.getConfig().virtualHost)
            pw.print(" checked");
        pw.print("></td></tr>");
        String password = service.getConfig().password != null ? SET_PASSWORD : "";
        
        pw.print("<tr><td>Admin password</td><td><input type=\"password\" name=\"password\" value=\"");
        pw.print(Utils.htmlEncode(password, false));
        pw.print("\"<br><input type=\"password\" name=\"password2\" value=\"");
        pw.print(Utils.htmlEncode(password, false));
        pw.print("\"></td></tr>");
        pw.print("<tr><td rowspan=\"2\">Binding addr</td><td><input type=\"text\" name=\"bind_addr\" value=\"");
        if(service.getConfig().bindAddr != null)
            pw.print(service.getConfig().bindAddr);
        pw.print("\"></td></tr>");
        pw.print("<tr><td><select onChange=\"updateBindAddr(this)\"><option>Custom</option>");
        for(Enumeration<NetworkInterface> en = NetworkInterface
                .getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                    .hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                pw.print("<option value=\"");
                pw.print(inetAddress.getHostAddress());
                pw.print("\"");
                if(inetAddress.getHostAddress().equals(service.getConfig().bindAddr))
                    pw.print(" selected");
                pw.print(">");
                pw.print(inetAddress.getHostName());
                pw.print("</option>");
            }
        }
        pw.print("<option value=\"0.0.0.0\"");
        if("0.0.0.0".equals(service.getConfig().bindAddr))
            pw.print(" selected");
        pw.print(">All interfaces</option>");
        pw.print("</select></td></tr>");
        pw.print("<tr><td colspan=\"2\">WebSocket &nbsp;&nbsp;<input type=\"checkbox\" name=\"websocket_enab\" value=\"true\"");
        if(service.getConfig().websocket_enab)
            pw.print(" checked");
        pw.print("> &nbsp;&nbsp;&nbsp; No new app &nbsp;<input type=\"checkbox\" name=\"lock_app\" value=\"true\"");
        if(service.getConfig().app_deploy_lock)
            pw.print(" checked");
        pw.print("></td</tr>");
        // TODO add backlog
        pw.print("<tr><td>Home dir</td><td><input type=\"text\" name=\"home_dir\" value=\"");
        pw.print(System.getProperty(Config.APP_HOME, ""));
        if(service.getConfig().useSD == false)
            pw.print("\" disabled");
        else
            pw.print("\"");
        pw.print("></td></tr>");
        pw.print("<tr><td><input type=\"submit\" value=\"Save\" colspan=\"2\"></td></tr>");
        pw.print("</table></form>");
        pw.print("<div>Upload web application (.war) or keystore</div><form name=\"upload_form\" method=\"POST\" action=\"settings\" enctype=\"multipart/form-data\">");
        pw.print("<input type=\"file\" name=\"app_file\" onchange=\"document.forms.upload_form.submit()\"></form>");
        printFooter(pw);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String message = "Okay";
        PrintWriter pw = resp.getWriter();
        printHtmlHead(pw);
        String contentType = req.getContentType();
        if(contentType != null
                && contentType.toLowerCase().indexOf("multipart/form-data") >= 0) {
            MultipartParser mpp = new MultipartParser(req, resp);
            String fileName = (String) mpp.getParameter("app_file+"
                    + MultipartParser.FILENAME);
            message = "Nothing uploaded";
            if(service.getConfig().app_deploy_lock) {
                message = "Deploying new apps is not allowed";
                fileName = null;
            }
            if(fileName != null) {
                int sp = fileName.lastIndexOf('\\');
                if(sp >= 0)
                    fileName = fileName.substring(sp + 1);
                sp = fileName.lastIndexOf('/');
                if(sp >= 0)
                    fileName = fileName.substring(sp + 1);
                
                Object data = mpp.getParameter("app_file");
                if(data instanceof byte[]) {
                    FileOutputStream fos = null;
                    if(fileName.toLowerCase().endsWith(".war"))
                        fos = new FileOutputStream(new File(service.getDeployFolder(),
                                fileName));
                    else if(TJWSService.KEYSTORE.equals(fileName)) {
                        fos = new FileOutputStream(new File(service.getKeyDir(), TJWSService.KEYSTORE));
                    } else
                        message = fileName + " isn't supported type";
                    if(fos != null) {
                        fos.write((byte[]) data);
                        fos.close();
                        message = fileName + " stored";
                    }
                }
            }
        } else {
            // update data
            String val = req.getParameter("root_app");
            if("-".equals(val))
                service.getConfig().rootApp = null;
            else
                service.getConfig().rootApp = val;
            service.getConfig().wwwFolder = req.getParameter("webroot");
            File wwwFolder = new File(service.getConfig().wwwFolder);
            if(wwwFolder.exists() == false || wwwFolder.isDirectory() == false) {
                service.getConfig().wwwFolder = null;
                message = "Invalid web folder";
            } else
                service.updateWWWServlet();
            val = req.getParameter("password");
            if(SET_PASSWORD.equals(val) == false) {
                if(val.length() > 0) {
                    if(val.equals(req.getParameter("password2")))
                        service.getConfig().password = val;
                    else
                        message = "Passwords are not match";
                } else if(service.getConfig().password != null)
                    service.getConfig().password = null;
                atjeews.updateRealm();
            }
            val = req.getParameter("bind_addr");
            if(val.length() > 0)
                service.getConfig().bindAddr = val;
            else
                service.getConfig().bindAddr = null;
            service.getConfig().virtualHost = Boolean.TRUE.toString().equals(
                    req.getParameter("virt_host"));
            val = req.getParameter("home_dir");
            if(val != null && val.length() > 0) {
                System.setProperty(Config.APP_HOME, val);
            } else
                System.getProperties().remove(Config.APP_HOME);
            service.getConfig().app_deploy_lock = Boolean.TRUE.toString().equals(req.getParameter("lock_app"));
            service.getConfig().websocket_enab = Boolean.TRUE.toString().equals(req.getParameter("websocket_enab"));
            service.setDeployFolder(null);
            service.initDeployDirectory();
            service.storeConfig();
        }
        pw.print("<center>" + message
                + ".</center><br><a href=\"/settings\">Go back<a> to settings");
        printFooter(pw);
    }
    
    @Override
    protected long getLastModified(HttpServletRequest req) {
        return -1;
    }
    
    private void printHtmlHead(PrintWriter pw) {
        pw.print("<HTML><HEAD><TITLE>SettingServlet - ");
        pw.print(MainActivity.APP_NAME);
        pw.print("</TITLE><meta name=\"viewport\" content=\"width=device-width, user-scalable=no\" />");
        pw.print("<SCRIPT>function updateBindAddr(sel) { if(sel.selectedIndex ==0) document.forms[0].bind_addr.value=''; else document.forms[0].bind_addr.value=sel.options[sel.selectedIndex].value;  }</SCRIPT>");
        pw.print("</HEAD><BODY bgcolor=\"#D1E9FE\">");
    }
    
    private void printFooter(PrintWriter pw) {
        pw.print("<CENTER>");
        pw.print(Serve.Identification.serverIdHtml);
        pw.print("<BR><A HREF=\"http://drogatkin.github.io\">Privacy Policy</A></CENTER></BODY></HTML>");
    }
}
