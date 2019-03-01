/** Copyright 2011 Dmitriy Rogatkin, All rights reserved.
 *  $Id: Main.java,v 1.48 2012/09/15 17:47:27 dmitriy Exp $
 */
package rogatkin.mobile.web;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Atjeews Android launcher of TJWS with administration support
 * 
 * @author Dmitriy Rogatkin
 * 
 */
public class Main extends Activity {
	public static final String APP_NAME = "Atjeews";

	public static final int APP_VER_MN = 5;

	public static final int APP_VER_MJ = 1;

	public static final boolean DEBUG = false;

	protected Config config;
	protected ArrayList<String> servletsList;
	protected String hostName;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		loadConfig();
		servletsList = new ArrayList<String>();
		// getWindow().setSoftInputMode(
		// WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// list deployed apps
		final ListView listView = (ListView) findViewById(R.id.listView1);
		TextView header = new TextView(this);
		header.setText(R.string.list_deployed);
		listView.addHeaderView(header);

		// process deployment request
		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final EditText surl = (EditText) findViewById(R.id.editText1);
				String userInput = surl.getText().toString();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(surl.getWindowToken(), 0);
				if (userInput.length() <= "http://".length()) {
					Toast.makeText(Main.this, "Please specify location URL of .war", Toast.LENGTH_SHORT).show();
					return;
				}
				if (config.app_deploy_lock) {
					Toast.makeText(Main.this, "Deploying new apps is locked", Toast.LENGTH_SHORT).show();
					return;
				}
				new AsyncTask<String, Void, Void>() {
					private String lastError;
					ProgressDialog dialog;

					@Override
					protected void onPostExecute(Void result) {
						dialog.dismiss();
						RCServ servCtrl = ((TJWSApp) getApplication())
								.getServiceControl();
						if (lastError != null) {
							AlertDialog.Builder builder = new AlertDialog.Builder(
									Main.this);
							builder.setMessage(String.format(getResources()
									.getString(R.string.alrt_problem),
									lastError));
							builder.setIcon(R.drawable.stop);
							builder.setTitle(R.string.t_error);
							builder.create().show();
						} else {
							if (lastError == null) {
								surl.setText("http://");
								try {
									fillServlets(servCtrl.getApps());
								} catch (Exception e) {
									reportProblem(e);
								}
							}
						}
						super.onPostExecute(result);
					}

					@Override
					protected void onPreExecute() {
						dialog = ProgressDialog.show(Main.this, "Deploying",
								"Please wait for few seconds..", true);
						super.onPreExecute();
					}

					@Override
					protected Void doInBackground(String... urls) {
						RCServ servCtrl = ((TJWSApp) getApplication())
								.getServiceControl();
						try {
							lastError = servCtrl.deployApp(urls[0]);
						} catch (NullPointerException npe) {
						} catch (RemoteException e) {
							reportProblem(e);
						}
						return null;
					}

				}.execute(userInput);
			}
		});
		// process rescan request
		button = (Button) findViewById(R.id.button2);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				RCServ servCtrl = ((TJWSApp) getApplication())
						.getServiceControl();
				try {
					if (servCtrl != null)
						fillServlets(servCtrl.rescanApps());
					else
						reportProblem(new Exception(
								"Can't obtain service control - null"));
				} catch (Exception e) {
					reportProblem(e);
				} // TODO consider AsyncTask
			}
		});

		listView.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, servletsList));
		registerForContextMenu(listView);

		CheckBox checkbox = (CheckBox) findViewById(R.id.checkStrt);
		checkbox.requestFocus();
		checkbox.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				if (((CheckBox) view).isChecked())
					start();
				else
					stop();
			}
		});

		((CheckBox) findViewById(R.id.checkBox2))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View view) {
						RCServ servCtrl = ((TJWSApp) getApplication())
								.getServiceControl();
						try {
							servCtrl.logging(((CheckBox) view).isChecked());
						} catch (Exception e) {
							reportProblem(e);
						}
					}
				});
		//updateUI();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		try {
			storeConfig();
		} catch (Exception e) {
			reportProblem(e);
		}
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUI();
	}

	private void updateUI() {
		if (DEBUG)
			Log.d(APP_NAME, "UI updated called");
		RCServ servCtrl = ((TJWSApp) getApplication()).getServiceControl();
		if (servCtrl != null) {
			updateStatus(servCtrl);
			try {
				updateAppsList(servCtrl.getApps(), true);
			} catch (RemoteException e) {
				if (DEBUG)
					Log.e(APP_NAME, "Updating apps list failed", e);
			}
			// Log.d(APP_NAME, "UPADTING       =====>"+hostName, new
			// Exception());
		} else if (DEBUG)
			Log.d(APP_NAME, "service not started");
		updateTitle();
	}

	private void updateStatus(RCServ servCtrl) {
		try {
			CheckBox startBtn = (CheckBox) findViewById(R.id.checkStrt);
			boolean stopped = servCtrl.getStatus() != TJWSServ.ST_RUN;
			if (DEBUG)
				Log.d(APP_NAME, "run "+stopped+", checking run "+startBtn.isChecked());
			//startBtn.setChecked(!stopped);
			if (stopped == false && hostName == null)
				start(servCtrl);
		} catch (Exception e) {
			reportProblem(e);
		}
	}

	void storeConfig() {
		config.load(this);
		config.logEnabled = ((CheckBox) findViewById(R.id.checkBox2))
				.isChecked();
		try {
			int port = Integer
					.parseInt(((EditText) findViewById(R.id.editPort))
							.getText().toString());
			if (port > 65536)
				throw new IllegalArgumentException("Port value " + port
						+ " is out of allowed range");
			else if (port < 1024) {
				try {
					ServerSocket ss = new ServerSocket(port);
					ss.close();
				} catch (Exception e) {
					throw new IllegalArgumentException("Port value " + port
							+ " is out of allowed range", e);
				}
			}
			config.port = port;
		} catch (Exception e) {
			throw new IllegalArgumentException("" + e, e);
		}
		config.ssl = ((CheckBox) findViewById(R.id.checkSSL)).isChecked();
		config.store(this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		if (info.position <= 0)
			return;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.app_ops, menu);
		ListView listView = (ListView) findViewById(R.id.listView1);
		String appName = (String) listView.getAdapter().getItem(info.position);
		if (appName != null)
			menu.setHeaderTitle(appName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		ListView listView = (ListView) findViewById(R.id.listView1);
		String appName = (String) listView.getAdapter().getItem(info.position);
		RCServ servCtrl = ((TJWSApp) getApplication()).getServiceControl();
		if (appName == null || servCtrl == null)
			return super.onContextItemSelected(item);
		try {
			switch (item.getItemId()) {
			case R.id.app_redeploy:
				fillServlets(servCtrl.redeployApp(appName));
				return true;
			case R.id.app_info:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(servCtrl.getAppInfo(appName));
				builder.setCancelable(true);
				builder.setTitle(R.string.t_info);
				builder.setIcon(R.drawable.info);
				AlertDialog alert = builder.create();
				alert.show();
				return true;
			case R.id.app_open:
				if (servCtrl.getStatus() != TJWSServ.ST_RUN || hostName == null) {
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alrt_noserver);
					builder.setIcon(R.drawable.stop);
					builder.setTitle(R.string.t_warning);
					builder.create().show();
					return true;
				}
				boolean ipv6 = !(hostName.indexOf(':') < 0);
				Intent browserIntent;
				if ("/settings".equals(appName))
					browserIntent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http" + (config.ssl ? "s" : "") + "://"
									+ (ipv6 ? "[" : "") + hostName
									+ (ipv6 ? "]" : "") + ":" + config.port
									+ "/settings"));
				else
					browserIntent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http"
									+ (config.ssl ? "s" : "")
									+ "://"
									+ (ipv6 ? "[" : "")
									+ hostName
									+ (ipv6 ? "]" : "")
									+ ":"
									+ config.port
									+ (appName.endsWith("/*") ? appName
											.substring(0, appName.length() - 2)
											: appName)));
				startActivity(browserIntent);
				return true;
			case R.id.app_remove:
				servCtrl.removeApp(appName);
				// coming through to stop
			case R.id.app_stop:
				fillServlets(servCtrl.stopApp(appName));
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		} catch (RemoteException e) {
			reportProblem(e);
		}
		return false;
	}

	protected void loadConfig() {
		if (config == null)
			config = new Config();
		config.load(this);
		((EditText) findViewById(R.id.editPort)).setText("" + config.port);
		((CheckBox) findViewById(R.id.checkSSL)).setChecked(config.ssl);
		((CheckBox) findViewById(R.id.checkBox2)).setChecked(config.logEnabled);
	}

	protected void fillServlets(List<String> newList) {
		updateAppsList(newList, true);
	}

	private void updateAppsList(List<String> newList, boolean notify) {
		servletsList.clear();
		if (newList == null) {
			if (DEBUG)
				Log.e(APP_NAME, "Error happened at retrieving apps list");
			return;
		}
		for (String servName : newList)
			servletsList.add(servName);
		if (notify) {
			ListView listView = (ListView) findViewById(R.id.listView1);
			((BaseAdapter) ((HeaderViewListAdapter) listView.getAdapter())
					.getWrappedAdapter()).notifyDataSetChanged();
		}
	}

	void updateTitle() {
		boolean running = hostName != null;
		((CheckBox) findViewById(R.id.checkSSL)).setEnabled(running == false);
		((EditText) findViewById(R.id.editPort)).setEnabled(running == false);
		if (running) {
			setTitle(APP_NAME + " [" + hostName + "]:" + config.port + "  @"
					+ (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "m");
			((CheckBox) findViewById(R.id.checkStrt)).setChecked(true);
	} else {
			setTitle(APP_NAME);
			((CheckBox) findViewById(R.id.checkStrt)).setChecked(false);
		}
	}

	private void start() {
		storeConfig();
		start(((TJWSApp) getApplication()).getServiceControl());
	}
	
	private void start(RCServ serv) {
		new AsyncTask<RCServ, Void, String>() {

			@Override
			protected void onPostExecute(String host) {
				hostName = host;
				updateTitle();
			}

			@Override
			protected String doInBackground(RCServ... sc) {
				try {
					return sc[0].start();
				} catch (Exception e) {
					reportProblem(e);
				}
				return null;
			}
		}.execute(serv);
	}

	private void stop() {
		RCServ servCtrl = ((TJWSApp) getApplication()).getServiceControl();
		if (servCtrl != null) {
			try {
				servCtrl.stop();
			} catch (RemoteException e) {
				reportProblem(e);
			}
			hostName = null;
		}
		updateTitle();
	}

	private void reportProblem(Throwable problem) {
		if (problem instanceof NullPointerException == false)
			if (DEBUG)
				Log.e(APP_NAME, "Unexpected problem:" + problem, problem);
		Toast.makeText(this, "" + problem, Toast.LENGTH_LONG).show();
	}

}