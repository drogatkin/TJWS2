/** Copyright 2012 Dmitriy Rogatkin, All rights reserved.
 *  $Id: TJWSApp.java,v 1.3 2012/09/15 17:47:27 dmitriy Exp $
 */
package rogatkin.mobile.web;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import java.lang.Thread.UncaughtExceptionHandler;

public class TJWSApp extends Application {
	protected RCServ servCtrl;

	@Override
	public void onCreate() {
		super.onCreate();
		if (Thread.getDefaultUncaughtExceptionHandler() == null)
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

				public void uncaughtException(Thread thread, Throwable ex) {
					if (Main.DEBUG)
						Log.e(Main.APP_NAME, "Unhandled exception " + ex
								+ " in the thread: " + thread, ex);
				}
			});
		startServe();
	}

	RCServ getServiceControl() {
		return servCtrl;
	}

	void startServe() {
		if (servCtrl != null) // sanity
			return;
		Intent serv = new Intent(this, TJWSServ.class);
		if (!isMyServiceRunning(TJWSServ.class))
			startService(serv);
		bindService(serv, new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				servCtrl = RCServ.Stub.asInterface(service);
				// can send notification to activities here
			}

			public void onServiceDisconnected(ComponentName name) {
				if (Main.DEBUG)
					Log.d(Main.APP_NAME, "Disconnected " + name);
				servCtrl = null;
			}
		}, BIND_AUTO_CREATE);
	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
