/** Copyright 2011 Dmitriy Rogatkin, All rights reserved.
 *  $Id: AndroidClassLoader.java,v 1.6 2012/05/23 05:08:15 dmitriy Exp $
 */
package rogatkin.mobile.web;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import android.util.Log;
import dalvik.system.DexClassLoader;

public class AndroidClassLoader extends DexClassLoader {

	ClassLoader parent;

	public AndroidClassLoader(URL[] classPath, ClassLoader parent) {
		super(convertFilePaths(classPath), assureClassStorage(classPath), null, parent);
		this.parent = parent;
	}

	private static String convertFilePaths(URL[] urls) {
		String result = "";
		String sep = System.getProperty("path.separator");
		for (URL url : urls) {
			result += url.getPath();
			result += sep;
		}
		// possibly trim last sep char
		if (Main.DEBUG)
			Log.d(Main.APP_NAME, "Class path:" + result);
		return result;
	}

	private static String assureClassStorage(URL[] classPath) {
		for (URL url : classPath) {
			String file = url.getFile();
			int wip = file.indexOf("WEB-INF");
			if (wip >= 0) {
				File dex_dir = new File(file.substring(0, wip)
						+ "META-INF/DEX/"+TJWSServ.SERVICE_NAME);
				//Log.d(Main.APP_NAME, "Dex dir:"+dex_dir+"  "+file);
				if (dex_dir.exists() == false) {
					dex_dir.mkdirs();
				}
				if (dex_dir.exists() && dex_dir.isDirectory())
					return dex_dir.getPath();
			}
		}
		throw new RuntimeException("Can't create dex temporary storage of "+Arrays.toString(classPath));
	}

	@Override
	protected URL findResource(String name) {
		// Log.d(Main.APP_NAME, "Request for resource:"+name);
		try {
			return super.findResource(name);
		} catch (Exception e) {

		}
		return parent.getResource(name);
	}
}
