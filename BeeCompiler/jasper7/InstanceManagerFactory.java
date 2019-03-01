/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.runtime;

import javax.servlet.ServletConfig;
import org.apache.jasper.tjws.SimpleInstanceManager;

import org.apache.tomcat.InstanceManager;

/**
 * @version $Id: InstanceManagerFactory.java,v 1.1 2011/06/04 04:59:07 dmitriy
 *          Exp $
 */
public class InstanceManagerFactory {
	
	static private InstanceManager cachedManager;  // same for all apps for now
	
	private InstanceManagerFactory() {
	}
	
	public static InstanceManager getInstanceManager(ServletConfig config) {
		InstanceManager instanceManager = (InstanceManager) config.getServletContext().getAttribute(InstanceManager.class.getName());
		if(instanceManager == null) {
			synchronized(InstanceManagerFactory.class) {
				if(cachedManager == null) {
					cachedManager = new SimpleInstanceManager();
				}
			}
			instanceManager = cachedManager;
			// throw new IllegalStateException("No
			// org.apache.tomcat.InstanceManager set in ServletContext");
		}
		return instanceManager;
	}
	
}
