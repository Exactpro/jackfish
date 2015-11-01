////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import java.util.Arrays;
import java.util.Map;

import com.exactprosystems.jf.api.app.ProxyApplication;

public class ProxySwingApp extends ProxyApplication
{
	@Override
	public boolean connect(int port, String jar, String work, String remoteClassName, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		System.out.println("SwingApp.connect() port=" + port + "  jar=" + jar + " work=" + work + " class=" + remoteClassName + " params=" + Arrays.toString(parameters.values().toArray()));
		return super.connect(port, jar, work, remoteClassName, driverParameters, parameters);
	}

	@Override
	public boolean start(int port, String jar, String work, String remoteClassName, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		System.out.println("SwingApp.start() port=" + port + "  jar=" + jar + " work=" + work + " class=" + remoteClassName + " params=" + Arrays.toString(parameters.values().toArray()));
		return super.start(port, jar, work, remoteClassName, driverParameters, parameters);
	}

	@Override
	public void stop() throws Exception
	{
		System.out.println("SwingApp.stop()");
		super.stop();
	}

}
