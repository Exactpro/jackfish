/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.jf.api.app;

import com.exactprosystems.jf.api.common.SerializablePair;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;


public abstract class ProxyApplication implements IApplication
{
	public static final String JREpathName 			= "JREpath";
	public static final String JVMparametersName 	= "JVMparameters";
	
	public static final String remoteLogName  		= "remoteLog";
	public static final String remoteLogLevelName	= "remoteLogLevel";
	public static final String remoteLogPatternName= "remoteLogPattern";

	private static final String LOCAL_HOST = "127.0.0.1";

	public ProxyApplication()
	{
	}

	@Override
	public void init(IApplicationPool pool, IApplicationFactory factory) throws Exception
	{
		this.pool = pool;
		this.factory = factory;
	}

   @Override
    public int reconnect(Map<String, String> parameters) throws Exception
    {
        this.process = null;
        int pid = this.service.connect(parameters);
		this.service.setPluginInfo(this.factory.getInfo());
		return pid;
    }

	@Override
	public SerializablePair<Integer, Integer> connect(ConnectionConfiguration configuration, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		return startOrConnect(configuration, driverParameters, parameters, false);
	}

	@Override
	public SerializablePair<Integer, Integer> start(ConnectionConfiguration configuration, Map<String, String> driverParameters, Map<String, String> parameters) throws Exception
	{
		return startOrConnect(configuration, driverParameters, parameters, true);
	}

	public SerializablePair<Integer,Integer> startOrConnect(ConnectionConfiguration configuration, Map<String, String> driverParameters, Map<String, String> parameters, boolean start) throws Exception
	{
	    String remoteHost = LOCAL_HOST;
	    int remotePort = 0;
        if (Str.IsNullOrEmpty(configuration.getRemoteHost())) {
            this.process = buildProcess(configuration, driverParameters, parameters);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream()))) {
                String outPort;
                while ((outPort = reader.readLine()) != null) {
                    if (outPort.startsWith(RemoteApplication.CONNECTED_PORT)) {
                        remotePort = Integer.parseInt(outPort.split(RemoteApplication.CONNECTED_PORT_DELIMITER)[1]);
                        break;
                    }
                }
            }

            Thread.sleep(1000);
        } else {
            remoteHost = configuration.getRemoteHost();
            remotePort = configuration.getRemotePort();
        }


		String remoteLog 		= driverParameters.get(remoteLogName);
		String remoteLogLevel 	= driverParameters.get(remoteLogLevelName);
		String remoteLogPattern	= driverParameters.get(remoteLogPatternName);

		remoteLog 			= remoteLog == null ? "remote.log" : remoteLog;
		remoteLogLevel 		= remoteLogLevel == null? "ALL" : remoteLogLevel;
		remoteLogPattern	= remoteLogPattern == null ? "%-5p %d{yyyy-MM-dd HH:mm:ss.SSS} %c{1}:%-3L-%m%n" : remoteLogPattern;
		
	    // connect to the service
	    Exception lastException = null;
	    for (int attempt = 0; attempt < 10; attempt++)
	    {
	    	try
	    	{
				Registry registry = LocateRegistry.getRegistry(remoteHost, remotePort);
				this.service = (IRemoteApplication) registry.lookup(serviceName);
						
				if (this.service != null)
				{
					break;
				}
	    	}
	    	catch (Exception e)
	    	{ 
	    		lastException = e;
	    	}
	    	
    		try
    		{
    		    if (process != null) {
                    process.exitValue();
                }
	    		break;
    		}
    		catch (IllegalThreadStateException e)
    		{
    			// nothing to do
    		}
	    	
	    	Thread.sleep(1000 + attempt * 100);
	    }
        if (this.service != null)
	    {
	    	try
	    	{
				this.service.createLogger(remoteLog, remoteLogLevel, remoteLogPattern);
                int pid = start ? this.service.run(parameters) : this.service.connect(parameters);
                this.service.setPluginInfo(this.factory.getInfo());
                return new SerializablePair<>(pid, remotePort);
	    	}
	    	catch (ServerException se) {
				tryStop();
				throw new Exception((se.getCause().getMessage()), se.getCause());
			}
	    	catch (Throwable t)
	    	{
				tryStop();
				throw t;
	    	}
	    }
	    else
	    {
	    	stop(false);
	    	if (lastException != null)
	    	{
	    		throw lastException;
	    	}
	    	throw new Exception(R.PROXY_APPLICATION_SERVICE_CANT_START_EXCEPTION.get());
	    }
	}

    private Process buildProcess(ConnectionConfiguration configuration, Map<String, String> driverParameters, Map<String, String> parameters) throws IOException {
        String fileSeparator 	= System.getProperty("file.separator");

        String javaRuntime  	= driverParameters.get(JREpathName);
        if (javaRuntime == null || javaRuntime.isEmpty())
        {
            javaRuntime  			= System.getProperty("java.home") + fileSeparator + "bin" + fileSeparator + "java";
        }

        // find the working directory
        File workDir = null;
        if (configuration.getWorkDirectory() != null)
        {
            workDir = new File(configuration.getWorkDirectory());
        }

        // compose all command-line parameters to launch another process
        List<String> commandLine = new ArrayList<>();
        add(commandLine, javaRuntime);

        String jvmParameters = driverParameters.get(JVMparametersName);
        StringBuilder classPath = new StringBuilder();
        String separator = System.getProperty("path.separator");
        classPath.append(configuration.getJar()).append(separator);

        if (jvmParameters != null)
        {
            String[] split = jvmParameters.trim().split(" ");
            List<String> additionalParameters = Arrays.asList(split);
            Iterator<String> iterator = additionalParameters.iterator();
            while (iterator.hasNext())
            {
                String next = iterator.next();
                if ("-cp".equals(next) || "-classpath".equals(next))
                {
                    classPath.append(iterator.next());
                }
                else
                {
                    add(commandLine, next);
                }
            }
        }

        addToClassPath(classPath, parameters);

        add(commandLine, "-cp");
        add(commandLine, classPath.toString());
        add(commandLine, RemoteApplication.class.getName());
        add(commandLine, configuration.getRemoteClassName());
        add(commandLine, String.valueOf(configuration.getStartPort()));

        System.out.println(commandLine);

        //command need be like this
        //java -cp jar.jar:another1.jar:another2.jar com.exactprosystems.jf.api.app.RemoteApplication remoteClassName port
        //		classpath								mainclass										arguments
        // launch the process
        Redirect errOutput = Redirect.appendTo(new File("remote_out.txt"));

        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder
            .redirectError(errOutput)
            .directory(workDir);

        return builder.start();
    }

	private void tryStop(){
		try
		{
			stop(false);
		}
		catch (Exception e)
		{

		}
	}

	
	@Override
	public void stop(boolean needKill) throws Exception
	{
		if (this.service != null)
		{
			try
			{
				this.service.stop(needKill);
			}
			catch(RemoteException e)
			{
				// if app was closed earlier, go to finally
			}
			finally
			{
				this.service = null;
			}
		}

		Thread.sleep(500);

		if (this.process != null)
		{
			this.process.destroy();
			this.process = null;
		}
	}
	
	@Override
	public IRemoteApplication service()
	{
		return this.service;
	}
	
	@Override
	public IApplicationPool getPool()
	{
		return this.pool;
	}
	
	@Override
	public IApplicationFactory	getFactory()
	{
		return this.factory;
	}

	protected void addToClassPath(StringBuilder sb, Map<String, String> parameters)
	{

	}

	private static void add(List<String> list, String str)
	{
		if (str != null)
		{
			list.add(str);
		}
	}

	private Process process;
	private IApplicationPool pool;
	private IApplicationFactory factory;
	private IRemoteApplication service;
}
