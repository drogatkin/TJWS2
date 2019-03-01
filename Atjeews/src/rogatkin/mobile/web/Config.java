/** Copyright 2012 Dmitriy Rogatkin, All rights reserved.
 * 
 */
package rogatkin.mobile.web;

import java.net.InetAddress;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
	
	public static final String APP_HOME = "atjeews.home";
	static final String P_PORT = "port";
	static final String P_SSL = "ssl";
	static final String P_ROOTAPP = "root_app";
	static final String P_PASSWRD = "password";
	static final String P_WEBROOT = "wwwroor";
	static final String P_VIRTUAL = "virtual";
	static final String P_BINDADDR = "bind_addr";
	static final String P_HOMEDIR = "home_dir";
	static final String P_APPLOCK = "applock";
	static final String P_WEBSOCKET = "websocket";
	static final String P_BACKLOG = "backlog";
	
	public InetAddress iadr;
	public int port;
	public boolean ssl;
	public boolean webSocketEnabled;
	public boolean app_deploy_lock;
	public boolean logEnabled;
	public boolean virtualHost;
	public boolean useSD = true;
	public String rootApp;
	public String wwwFolder;
	public String password; // admin password
	public String bindAddr;
	public int backlog;
	
	protected void store(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(Main.APP_NAME, Context.MODE_WORLD_READABLE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("log_enable", logEnabled);
		editor.putBoolean(P_SSL, ssl);
		editor.putInt(P_PORT, port <= 0 ? 8080 : port);
		editor.putInt(P_BACKLOG, backlog <= 0 ? 60 : backlog);
		editor.putString(P_PASSWRD, password);
		editor.putBoolean(P_APPLOCK, app_deploy_lock);
		editor.putBoolean(P_WEBSOCKET, webSocketEnabled);
		if (System.getProperty(APP_HOME) != null)
			editor.putString(P_HOMEDIR, System.getProperty(APP_HOME));
		else
			editor.remove(P_HOMEDIR);
		if (bindAddr == null)
			editor.remove(P_BINDADDR);
		else
			editor.putString(P_BINDADDR, bindAddr);
		editor.putString(P_WEBROOT, wwwFolder);
		if (rootApp == null)
			editor.remove(P_ROOTAPP);
		else
			editor.putString(P_ROOTAPP, rootApp);
		editor.putBoolean(P_VIRTUAL, virtualHost);
		editor.commit();
	}
	
	protected void load(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(Main.APP_NAME, Context.MODE_WORLD_READABLE);
		port = prefs.getInt(P_PORT, 8080);
		ssl = prefs.getBoolean(P_SSL, false);
		backlog = prefs.getInt(P_BACKLOG, 60);
		logEnabled = prefs.getBoolean("log_enable", false);
		bindAddr = prefs.getString(P_BINDADDR, null);
		virtualHost = prefs.getBoolean(P_VIRTUAL, false);
		webSocketEnabled = prefs.getBoolean(P_WEBSOCKET, false);
		app_deploy_lock = prefs.getBoolean(P_APPLOCK, false);
		String home = prefs.getString(P_HOMEDIR, null);
		if (home != null) {
			System.setProperty(APP_HOME, home);
			useSD = false;
		} else {
			System.getProperties().remove(APP_HOME);
			useSD = true;
		}
		rootApp = prefs.getString(P_ROOTAPP, null);
		wwwFolder = prefs.getString(P_WEBROOT, "/");
		password = prefs.getString(P_PASSWRD, null);
	}
}
