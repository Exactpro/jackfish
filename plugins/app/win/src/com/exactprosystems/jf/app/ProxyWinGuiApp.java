////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import com.exactprosystems.jf.api.app.ProxyApplication;

import java.util.Arrays;
import java.util.Map;

public class ProxyWinGuiApp extends ProxyApplication
{
	public ProxyWinGuiApp() throws Exception
	{
		if (!isWindows())
		{
			throw new Exception("This adapter needs Windows.");
		}
	}
	
	@Override
	public boolean connect(int port, String jar, String work, String remoteClassName, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		System.out.println("WinGuiApp.connect() " +port + "  " + Arrays.toString(parameters.values().toArray()));
		return super.connect(port, jar, work, remoteClassName, driverParameters, parameters);
	}

	@Override
	public boolean start(int port, String jar, String work, String remoteClassName, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		System.out.println("WinGuiApp.start() " +port + "  " + Arrays.toString(parameters.values().toArray()));
		return super.start(port, jar, work, remoteClassName, driverParameters, parameters);
	}

	@Override
	public void stop() throws Exception
	{
		System.out.println("WinGuiApp.stop()");
		super.stop();
	}

	private boolean isWindows()
	{
		String OS = System.getProperty("os.name"); 
		if (OS != null)
		{
			return OS.startsWith("Windows");
		}

		return false;
	}
}
