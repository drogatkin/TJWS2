package org.apache.jasper.tjws;

import java.lang.reflect.InvocationTargetException;
import javax.naming.NamingException;
import org.apache.tomcat.InstanceManager;

public class SimpleInstanceManager implements InstanceManager {
	
	public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
		return clazz.newInstance();
	}
	
	public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
		return Class.forName(className).newInstance();
	}
	
	public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
		// System.err.printf("Requested new instance as %s with loader %s and
		// parent %s%n", fqcn, classLoader, classLoader.getParent());
		try {
			return Class.forName(fqcn, true, classLoader).newInstance();
		} catch(ClassNotFoundException cnfe) {
			// System.err.printf("Exceprion %s%n", cnfe);
			// trying parent loader for Android
		}
		return Class.forName(fqcn, true, classLoader.getParent()).newInstance();
	}
	
	public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
		// System.err.printf("New instance for object %s", o);
	}
	
	public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
		// System.err.printf("Destroying object %s", o);
	}
	
}