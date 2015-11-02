////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.app;

import org.w3c.dom.Document;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class RemoteApplication implements IRemoteApplication
{
	public static void main(String[] args)
	{
		try
		{
			if (args.length < 2)
			{
				throw new Exception("Too few arguments :" + args.length);
			}
			System.err.println("Starting remote");

			String mainClassName = removeExtraQuotes(args[0]);
			String portSt = removeExtraQuotes(args[1]);
			String[] argsApp = Arrays.copyOfRange(args, 2, args.length);
			
			int port = Integer.parseInt(portSt);

			System.err.println("mainClass  = " + mainClassName);
			System.err.println("port       = " + portSt);
			System.err.println("other args = " + Arrays.toString(argsApp));
			

			System.err.println("Creating remote service...");
			IRemoteApplication service = objectFromClassName(mainClassName, IRemoteApplication.class);
			System.err.println("... creating complete");

			services.add(service); // to keep strong reference from GC
			
			System.err.println("Registering remote stub...");
			Remote stub = UnicastRemoteObject.exportObject(service, 0);
			final Registry registry = LocateRegistry.createRegistry(port);
			registry.rebind(IApplication.serviceName, stub);
			System.err.println("... registering complete");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	
	

	@Override
	public final void createLogger(String logName, String serverLogLevel, String serverLogPattern) throws RemoteException
	{
		try 
		{
			exceptionIfNull(logName, 			"logName", "createLogger");
			exceptionIfNull(serverLogLevel, 	"serverLogLevel", "createLogger");
			exceptionIfNull(serverLogPattern, 	"serverLogPattern", "createLogger");
			
			createLoggerDerived(logName, serverLogLevel, serverLogPattern);
		}
		catch (Exception e)
		{
			String msg = String.format("Error createLogger(%s, %s, %s)", logName, serverLogLevel, serverLogPattern);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}



	@Override
	public final void connect(Map<String, String> args) throws RemoteException
	{
		try 
		{
			exceptionIfNull(args, "args", "run");

			connectDerived(args);
		}
		catch (Exception e)
		{
			String msg = String.format("Error connect(%s)", args == null ? "null" : Arrays.toString(args.entrySet().toArray()));
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public final void run(Map<String, String> args) throws RemoteException
	{
		try 
		{
			exceptionIfNull(args, "args", "run");

			runDerived(args);
		}
		catch (Exception e)
		{
			String msg = String.format("Error run(%s)", args == null ? "null" : Arrays.toString(args.entrySet().toArray()));
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public final void stop() throws RemoteException
	{
		try 
		{
			stopDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error stop()");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}


	@Override
	public final void refresh() throws RemoteException
	{
		try 
		{
			refreshDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error refresh()");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public final Collection<String>	titles() throws RemoteException
	{
		try 
		{
			return titlesDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error titles()");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public final void newInstance(Map<String, String> args) throws Exception
	{
		try
		{
			exceptionIfNull(args, "args", "newInstance");

			newInstanceDerived(args);
		}
		catch (Exception e)
		{
			String msg = String.format("Error newInstance(%s)", args == null ? "null" : Arrays.toString(args.entrySet().toArray()));
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public final String switchTo(String title) throws RemoteException
	{
		try 
		{
			exceptionIfNull(title, "title", "switchTo");

			return switchToDerived(title);
		}
		catch (Exception e)
		{
			String msg = String.format("Error switchTo(%s)", title);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}


	@Override
	public final Collection<String> findAll(Locator owner, Locator element) throws RemoteException
	{
		try 
		{
			exceptionIfNull(element, "element", "findAll");
			
			return findAllDerived(owner, element);
		}
		catch (Exception e)
		{
			String msg = String.format("Error findAll(%s, %s)", owner, element);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}
	
	@Override
	public final Locator getLocator (Locator owner, ControlKind controlKind, int x, int y) throws RemoteException
	{
		try 
		{
			exceptionIfNull(controlKind, 	"controlKind", "getLocator");

			return getLocatorDerived(owner, controlKind, x, y);
		}
		catch (Exception e)
		{
			String msg = String.format("Error getLocator(%s, %d, %d)", controlKind, x, y);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}
	
	
	@Override
	public final ImageWrapper getImage(Locator owner, Locator element) throws RemoteException
	{
		try 
		{
			exceptionIfNull(element, 	"element", "getImage");

			return getImageDerived(owner, element);
		}
		catch (Exception e)
		{
			String msg = String.format("Error operate(%s, %s)", owner, element);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}
	
	@Override
	public final OperationResult operate(Locator owner, Locator element, Locator rows, Locator header, Operation operation) throws RemoteException
	{
		try 
		{
			exceptionIfNull(element, 	"element", "operate");
			exceptionIfNull(operation, "operation", "operate");

			return operateDerived(owner, element, rows, header, operation);
		}
		catch (Exception e)
		{
			String msg = String.format("Error operate(%s, %s, %s)", owner, element, operation);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public int closeAll(Locator element, Collection<LocatorAndOperation> operations) throws RemoteException
	{
		try 
		{
			exceptionIfNull(element, 		"element", "closeAll");
			exceptionIfNull(operations, "operations", "closeAll");

			return closeAllDerived(element, operations);
		}
		catch (Exception e)
		{
			String msg = String.format("Error closeAll(%s, %s)", element, Arrays.toString(operations.toArray()));
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public String closeWindow() throws RemoteException
	{
		try
		{
			return closeWindowDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error closeWindow");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public Document getTree(Locator owner) throws RemoteException
	{
		try
		{
			return getTreeDerived(owner);
		}
		catch (Exception e)
		{
			String msg = String.format("Error getTree(%s)", owner);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public void startGrabbing() throws RemoteException
	{
		try
		{
			startGrabbingDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error start grabbing");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}

	@Override
	public void endGrabbing() throws RemoteException
	{
		try
		{
			endGrabbingDerived();
		}
		catch (Exception e)
		{
			String msg = String.format("Error stop grabbing");
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}
	
	public void highlight	(Locator owner, String xpath) throws RemoteException
	{
		try
		{
			highlightDerived(owner, xpath);
		}
		catch (Exception e)
		{
			String msg = String.format("Error highlight(%s, %s)", owner, xpath);
			throw new ProxyException(msg, e.getMessage(), e);
		}
	}


	protected abstract void createLoggerDerived(String logName, String serverLogLevel, String serverLogPattern) throws Exception;
	
	protected abstract void connectDerived(Map<String, String> args) throws Exception;

	protected abstract void runDerived(Map<String, String> args) throws Exception;

	protected abstract void stopDerived() throws Exception;
	
	protected abstract void refreshDerived() throws Exception;

	protected abstract Collection<String> titlesDerived() throws Exception;

	protected abstract String switchToDerived(String title) throws Exception;

	protected abstract Collection<String> findAllDerived(Locator owner, Locator element) throws Exception;
	
	protected abstract Locator getLocatorDerived (Locator owner, ControlKind controlKind, int x, int y) throws Exception;

	protected abstract ImageWrapper getImageDerived(Locator owner, Locator element) throws Exception;

	protected abstract OperationResult operateDerived(Locator owner, Locator element, Locator rows, Locator header, Operation operation) throws Exception;

	protected abstract void newInstanceDerived(Map<String,String> args) throws Exception;
	
	protected abstract int closeAllDerived(Locator element, Collection<LocatorAndOperation> operations) throws Exception;

	protected abstract String closeWindowDerived() throws Exception;

	protected abstract Document getTreeDerived(Locator owner) throws Exception;

	protected abstract void startGrabbingDerived() throws Exception;

	protected abstract void endGrabbingDerived() throws Exception;

	protected abstract void highlightDerived(Locator owner, String xpath) throws Exception;

	private static String removeExtraQuotes(String string)
	{
		if (string != null)
		{
			if (string.startsWith("\""))
			{
				string = string.substring(1);
			}
			
			if (string.endsWith("\""))
			{
				string = string.substring(0, string.length() - 1);
			}

			return string;
		}
			
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T > T objectFromClassName(String name, Class<T> baseType) 	throws Exception
	{
		Class<?> type = Class.forName(name);
		
		if (!baseType.isAssignableFrom(type))
		{
			throw new Exception("class '" + name + "' is not assignable from " + baseType.getName());
		}

		return (T)type.newInstance();
	}

	private void exceptionIfNull(Object object, String message, String methodName)
	{
		if (object == null)
		{
			throw new NullPointerException("Parameter '" + message + "' is null in call '" + methodName + "'");
		}
	}

	private static List<IRemoteApplication> services = new ArrayList<IRemoteApplication>();

}
