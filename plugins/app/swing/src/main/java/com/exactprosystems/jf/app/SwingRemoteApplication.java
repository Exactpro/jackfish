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

package com.exactprosystems.jf.app;

import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.api.common.ProcessTools;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.app.FeatureNotSupportedException;
import com.exactprosystems.jf.api.error.app.NullParameterException;
import com.exactprosystems.jf.api.error.app.WrongParameterException;
import netx.jnlp.Launcher;
import netx.jnlp.runtime.ApplicationInstance;
import netx.jnlp.runtime.JNLPRuntime;
import org.apache.log4j.*;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.ComponentMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.ComponentFixture;
import org.w3c.dom.Document;
import sun.awt.AppContext;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SwingRemoteApplication extends RemoteApplication
{
	private Logger logger = null;
	private Robot currentRobot;

	private SwingOperationExecutor operationExecutor;
	private PluginInfo             info;

	@Override
	protected void createLoggerDerived(String logName, String serverLogLevel, String serverLogPattern) throws Exception
	{
		try
		{
			logger = Logger.getLogger(SwingRemoteApplication.class);

			Layout layout = new PatternLayout(serverLogPattern);
			Appender appender = new FileAppender(layout, logName);
			logger.addAppender(appender);
			logger.setLevel(Level.toLevel(serverLogLevel, Level.ALL));

			MatcherSwing.setLogger(logger);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("createLoggerDerived(%s, %s,%s)", logName, serverLogLevel, serverLogPattern));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

    @Override
    protected void setPluginInfoDerived(PluginInfo info) throws Exception
    {
        this.info = info;
        this.operationExecutor.setPluginInfo(info);
    }

	@Override
	public Serializable getProperty(String name, Serializable prop) throws RemoteException
	{
	    if (this.operationExecutor != null)
	    {
            switch (name)
            {
                case SwingAppFactory.propertyTitle:
                    return this.operationExecutor.currentFrame().getName();
            }
	    }
        return null;
	}

    @Override
    public void setProperty(String name, Serializable prop) throws RemoteException
    {
    }

	@Override
	protected int connectDerived(Map<String, String> args) throws Exception
	{
		String url = args.get(SwingAppFactory.urlName);

		logger.debug("Connecting to web start application: url=" + url);
		
		try
		{
			// java -jar netx.jar -verbose -nosecurity  -Xtrustall -Xnofork http://pathTo.jnlp
			
			JNLPRuntime.setDebug(true);
			JNLPRuntime.setSecurityEnabled(false);
			JNLPRuntime.setBaseDir(new File("."));

			JNLPRuntime.initialize();
			logger.debug("Runtime init has done.");

			Launcher launcher = new Launcher();

			ApplicationInstance app = null;

			try
			{
				app = launcher.launch(new URL(url));
				logger.debug("connected to " + app);
			}
			catch (Throwable t)
			{
				logger.error(t.getMessage(), t);
			}
			if (app == null)
			{
				throw new Exception(R.SWING_REMOTE_APP_CANT_INIT_APP.get());
			}

			this.currentRobot = new RobotListener(BasicRobot.robotWithCurrentAwtHierarchy());
			this.operationExecutor = new SwingOperationExecutor(this.currentRobot, this.logger, super.useTrimText, this.appContextFromThreadGroup(app.getThreadGroup()));
		}
		catch (Exception e)
		{
			logger.error("connectDerived. keys : " + args.keySet() + " , value : " + args.values());
			logger.error(e.getMessage(), e);
			throw e;
		}

		logger.debug("Application has been connected");
		
		return ProcessTools.currentProcessId();
	}

	@Override
	protected int runDerived(Map<String, String> args) throws Exception
	{
		String mainClass = args.get(SwingAppFactory.mainClassName);
		if (Str.IsNullOrEmpty(mainClass) || mainClass.equals("null"))
		{
			throw new NullParameterException("MainClass");
		}

		String jar = args.get(SwingAppFactory.jarName);
		if (Str.IsNullOrEmpty(jar) || jar.equals("null"))
		{
			throw new NullParameterException("Jar");
		}
		String arg = args.get(SwingAppFactory.argsName);

		logger.debug("Launching application: class=" + mainClass + " jar=" + jar + " arg=" + arg);

        try
		{
			List<URL> urls = new ArrayList<URL>();
			urls.add(new URL("file:" + jar));

			ClassLoader parent = getClass().getClassLoader();
			@SuppressWarnings("resource") URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[]{}), parent);

			Class<?> applicationType = classLoader.loadClass(mainClass);
			Method mainMethod = applicationType.getMethod("main", String[].class);
			mainMethod.invoke(null, new Object[]{arg == null ? null : new String[]{arg}});

			this.currentRobot = new RobotListener(BasicRobot.robotWithCurrentAwtHierarchy());
			this.operationExecutor = new SwingOperationExecutor(this.currentRobot, this.logger, super.useTrimText, null);
		}
		catch (Exception e)
		{
			logger.error("connectDerived. keys : " + args.keySet() + " , value : " + args.values());
			logger.error(e.getMessage(), e);
			throw e;
		}

		logger.debug("Application has been launched");

		return ProcessTools.currentProcessId();
	}

	@Override
	protected void stopDerived(boolean needKill) throws Exception
	{
		try
		{
		}
		catch (Exception e)
		{
			logger.error("stopDerived()");
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected void refreshDerived() throws Exception
	{
		throw new FeatureNotSupportedException("refresh");
	}

	@Override
	protected String getAlertTextDerived() throws Exception
	{
		throw new FeatureNotSupportedException("getAlertText");
	}

	@Override
	protected void navigateDerived(NavigateKind kind) throws Exception
	{
		throw new FeatureNotSupportedException("navigate");
	}

	@Override
	protected void setAlertTextDerived(String text, PerformKind performKind) throws Exception
	{
		throw new FeatureNotSupportedException("setAlertText");
	}

	@Override
	protected Collection<String> titlesDerived() throws Exception
	{
		try
		{
			Collection<String> list = new ArrayList<String>();
			Collection<Component> allDialogs = this.currentRobot.finder().findAll(new ComponentMatcher()
			{
				@Override
				public boolean matches(Component component)
				{
					if (component != null && component.isShowing() && (component instanceof JFrame || component instanceof JDialog))
					{
						return true;
					}
					return false;
				}
			});
			for (Component dialog : allDialogs)
			{
				if (dialog instanceof JFrame)
				{
					list.add(((Frame) dialog).getTitle());
				}
				if (dialog instanceof JDialog)
				{
					list.add(((Dialog) dialog).getTitle());
				}
			}

			return list;
		}
		catch (Exception e)
		{
			logger.error("titlesDerived()");
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected void resizeDerived(Resize resize, int height, int width) throws Exception
	{
		try
		{
			Component component = this.operationExecutor.currentFrame();
			if (component instanceof JFrame)
			{
				resizeWindow((JFrame) component, resize, height, width);
			}
		}
		catch (Exception e)
		{
			logger.error(String.format("resizeDerived(%s,%s)", height, width));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
    protected void resizeDialogDerived(Locator element, Resize resize, int height, int width) throws Exception
    {
		try
		{
			ComponentFixture<Component> componentFixture = this.operationExecutor.find(null, element);
			if (componentFixture.target instanceof JFrame)
			{
				resizeWindow((JFrame) componentFixture.target, resize, height, width);
			}
			if (componentFixture.target instanceof JDialog)
			{
				resizeWindow((JDialog) componentFixture.target, resize, height, width);
			}
		}
		catch (Exception e)
		{
			logger.error(String.format("resizeDerived(%s,%s)", height, width));
			logger.error(e.getMessage(), e);
			throw e;
		}
    }

	@Override
	protected Dimension getDialogSizeDerived(Locator owner) throws Exception
	{
		ComponentFixture<Component> fixture = this.operationExecutor.find(null, owner);
		if (fixture.component() instanceof Window)
		{
			return fixture.component().getSize();
		}
		else
		{
			throw new FeatureNotSupportedException(String.format(R.SWING_REMOTE_APP_SELF_IS_NOT_A_WINDOW.get(), owner));
		}
	}

	@Override
	protected Point getDialogPositionDerived(Locator owner) throws Exception
	{
		ComponentFixture<Component> fixture = this.operationExecutor.find(null, owner);
		if (fixture.component() instanceof Window)
		{
			return fixture.component().getLocation();
		}
		else
		{
			throw new FeatureNotSupportedException(String.format(R.SWING_REMOTE_APP_SELF_IS_NOT_DIALOG_OR_FRAME.get(), owner));
		}
	}

	@Override
	protected String switchToDerived(final Map<String, String> criteria, boolean softCondition) throws Exception
	{
		throw new FeatureNotSupportedException("switchTo");
	}

	@Override
	protected void switchToFrameDerived(Locator owner, Locator element) throws Exception
	{
		throw new FeatureNotSupportedException("switchToFrame");
	}

	@Override
	protected Collection<String> findAllDerived(Locator owner, Locator element) throws Exception
	{
		try
		{
			List<String> res = new ArrayList<String>();
			List<ComponentFixture<Component>> components = this.operationExecutor.findAll(owner, element);
			
			for (ComponentFixture<Component> component : components)
			{
			    StringBuilder stringBuilder = new StringBuilder("" + component.target);
				if (component.target instanceof JComboBox)
				{
					JComboBox<?> combobox = (JComboBox<?>)component.target;
					for (int i = 0; i < combobox.getModel().getSize(); i++)
					{
						stringBuilder.append('\n').append("value=" + combobox.getModel().getElementAt(i));
					}
				}
                res.add(stringBuilder.toString());
			}
			return res;
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("findAllDerived (%s,%s)", owner, element));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected ImageWrapper getImageDerived(final Locator owner, final Locator element) throws Exception
	{
		try
		{
			final BufferedImage[] images = new BufferedImage[1];
			final Exception[] exceptions = new Exception[1];
			ComponentFixture<Component> component = null;
			if (element != null)
			{
				component = operationExecutor.find(owner, element);
			}
			final ComponentFixture<Component> finalComponent = component;
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Component target;
						if (finalComponent == null)
						{
							target = operationExecutor.currentFrame();
							if (target == null)
							{
								Rectangle desktopRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
								images[0] = new java.awt.Robot().createScreenCapture(desktopRect);
								return;
							}
						}
						else
						{
							target = finalComponent.target;
						}
						logger.debug("target  : " + target);
						logger.debug("target.getClass()  : " + target.getClass());
						logger.debug("target.getWith()  : " + target.getWidth());
						logger.debug("target.getHeight()  : " + target.getHeight());
						BufferedImage image = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_INT_RGB);
						logger.debug("image : " + image);
						Graphics graphics = image.getGraphics();
						logger.debug("Graphics : " + graphics);
						target.paintAll(graphics); // alternately use .printAll(..)
						images[0] = image;
					}
					catch (Exception e)
					{
						exceptions[0] = e;
					}
				}
			});
			if (exceptions[0] != null)
			{
				throw exceptions[0];
			}
			return new ImageWrapper(images[0]);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("getImageDerived(%s,%s)", owner, element));
			logger.error(e.getMessage(), e);
			throw e;
		}

	}

	@Override
	protected Rectangle getRectangleDerived(Locator owner, Locator element) throws Exception
	{
		try
		{
			ComponentFixture<Component> component;
			if (element == null)
			{
				component = new ComponentFixture<Component>(this.currentRobot, this.operationExecutor.currentFrame())
				{
					@Override
					protected boolean requireShowing()
					{
						return super.requireShowing();
					}
				};
			}
			else
			{
				component = this.operationExecutor.find(owner, element);
			}
			return this.operationExecutor.getRectangle(component);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("getRectangleDerived(%s, %s)", owner, element));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected OperationResult operateDerived(Locator owner, Locator element, Locator rows, Locator header, Operation operation) throws Exception
	{
		try
		{
			return operation.operate(this.operationExecutor, owner, element, rows, header);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("operateDerived(%s,%s,%s,%s,%s)", owner, element, rows, header, operation));
			logger.error("EXCEPTION : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}

	@Override
	protected CheckingLayoutResult checkLayoutDerived(Locator owner, Locator element, Spec spec) throws Exception
	{
		try
		{
			return spec.perform(this.operationExecutor, owner, element);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("checkLayoutDerived(%s,%s,%s)", owner, element, spec));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected void newInstanceDerived(Map<String, String> args) throws Exception
	{
		throw new FeatureNotSupportedException("newInstance");
	}

	@Override
	protected int closeAllDerived(Locator element, Collection<LocatorAndOperation> operations) throws Exception
	{
		try
		{
			int closed = 0;
			logger.debug("operations count : " + operations.size());
			logger.debug("element : " + element.toString());
			
			Component root = this.operationExecutor.fromOwner(null);
			ComponentFixture<Component> rootFixture = this.operationExecutor.getFixture(root);
			
			List<ComponentFixture<Component>> dialogs = this.operationExecutor.findAll(ControlKind.Any, rootFixture, element);
			logger.debug("found dialogs : " + dialogs.size());

			for (ComponentFixture<Component> dialog : dialogs)
			{	
				if (dialog == null)
				{
					continue;
				}
				if (dialog.target instanceof Window)
				{
					final Window wnd = (Window) dialog.target;
					logger.debug("close window : " + wnd.getName());
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							wnd.dispatchEvent(new WindowEvent(wnd, WindowEvent.WINDOW_CLOSING));
						}
					});
					closed++;
				}
				else
				{
					logger.debug("close other: " + dialog.target);
					for (LocatorAndOperation pair : operations)
					{
						Locator locator = pair.getLocator();
	
						List<ComponentFixture<Component>> components = this.operationExecutor.findAll(locator.getControlKind(), dialog, locator);
						if (components.size() == 1)
						{
							ComponentFixture<Component> component = components.get(0);
							Operation operation = pair.getOperation();
							operation.operate(this.operationExecutor, locator, component);
	
						}
					}
					closed++;
				}
			}

			return closed;
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("closeAllDerived(%s,%s)", element, operations));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected String closeWindowDerived() throws Exception
	{
		throw new FeatureNotSupportedException("closeWindow()");
	}

	@Override
	protected void startNewDialogDerived() throws Exception
	{
		this.operationExecutor.clearModifiers();
	}

	@Override
	protected void moveWindowDerived(int x, int y) throws Exception
	{
		try
		{
			Component component = this.operationExecutor.currentFrame();
			component.setLocation(x, y);
		}
		catch (Exception e)
		{
			logger.error(String.format("moveWindow(%s,%s)", x, y));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected void moveDialogDerived(Locator owner, int x, int y) throws Exception
	{
		try
		{
			ComponentFixture<Component> fixture = this.operationExecutor.find(null, owner);

			if (fixture.component() instanceof Window)
			{
				fixture.component().setLocation(x, y);
			}
			else
			{
				throw new FeatureNotSupportedException(String.format(R.SWING_REMOTE_APP_SELF_IS_NOT_A_WINDOW.get(), owner));
			}
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("moveDialogDerived(%s, %s, %s)", owner, x, y));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	protected Document getTreeDerived(Locator owner) throws Exception
	{
		try
		{
			Component component = this.operationExecutor.fromOwner(owner);
			return MatcherSwing.createDocument(this.info, component, false, true);
		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error(String.format("getTreeDerived(%s)", owner));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	//region private method
	private void resizeWindow(Window window, Resize resize, int height, int width) throws RemoteException
	{
		if (resize != null)
		{
			if(window instanceof JFrame)
			{
				JFrame frame = (JFrame) window;
				switch (resize)
				{
					case Maximize:
						logger.debug("Change state to maximized");
						frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
						logger.debug("Current state is " + frame.getExtendedState());
						break;
					case Minimize:
						logger.debug("Change state to minimize");
						frame.setExtendedState(JFrame.ICONIFIED);
						logger.debug("Current state is " + frame.getExtendedState());
						break;
					case Normal:
						logger.debug("Change state to normal");
						frame.setExtendedState(JFrame.NORMAL);
						logger.debug("Current state is " + frame.getExtendedState());
						break;
				}
			}
			if(window instanceof JDialog)
			{
				throw new WrongParameterException(R.SWING_REMOTE_APP_USE_DIALOG_RESIZE.get());
			}
		}
		else
		{
			logger.debug("Change state via width and height");
			window.setSize(width, height);
		}
	}

	private AppContext appContextFromThreadGroup(ThreadGroup group)
	{
		for (AppContext appContext : AppContext.getAppContexts())
		{
			if (appContext.getThreadGroup().equals(group))
			{
				return appContext;
			}
		}
		return null;
	}
	//endregion

}
