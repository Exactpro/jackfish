////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.api.common.ParametersKind;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class WebAppFactory implements IApplicationFactory
{
    public static final String  helpFileName          = "help.txt";

    public static final String   logLevel             = "LogLevel";
    public final static String   jreExecName          = "jreExec";
    public final static String   jreArgsName          = "jreArgs";

    public static final String   chromeDriverPathName     = "ChromeDriverPath";
    public static final String   geckoDriverPathName      = "GeckoDriverPath";
    public static final String   ieDriverPathName         = "IEDriverPath";
    public static final String   chromeDriverBinary       = "ChromeDriverBinary";
    public static final String   firefoxProfileDir        = "FirefoxProfileDirectory";
    public static final String   usePrivateMode           = "UsePrivateMode";

    public final static String   browserName              = "Browser";
    public final static String   urlName                  = "URL";
    public static final String   whereOpenName            = "WhereOpen";

    public static final String   tabName                  = "Tab";

    public static final String   propertyUrlName          = "URL";
    public static final String   propertyTitle            = "Title";
    public static final String   propertyAllTitles        = "AllTitles";
    public static final String   propertyCookie           = "Cookie";
    public static final String   propertyAllCookies       = "AllCookies";
    public static final String   propertyAddCookie        = "AddCookie";
    public static final String   propertyRemoveCookie     = "RemoveCookie";
    public static final String   propertyRemoveAllCookies = "RemoveAllCookies";
	
	private static String[] empty = {  };

	private static ControlKind[] supportedControls = 
		{ 
			ControlKind.Any, ControlKind.Wait, ControlKind.Button, ControlKind.CheckBox, ControlKind.ComboBox, ControlKind.Dialog,
			ControlKind.Frame, ControlKind.Image, ControlKind.Label, ControlKind.ListView, ControlKind.Panel,
			ControlKind.ProgressBar, ControlKind.RadioButton, ControlKind.Row, ControlKind.Slider,
			ControlKind.Spinner, ControlKind.Table, ControlKind.TextBox, ControlKind.ToggleButton,
		};

	private IGuiDictionary dictionary = null;

	public enum WhereToOpen 
	{
	    OpenInTab,
	    OpenInWindow,
	    OpenNewUrl;
	    
	    public static String[] names()
	    {
	        return new String[] { OpenInTab.name(), OpenInWindow.name(), OpenNewUrl.name() };
	    }
	}


	//----------------------------------------------------------------------------------------------
	// IFactory
	//----------------------------------------------------------------------------------------------
	@Override
	public String[] wellKnownParameters(ParametersKind kind)
	{
		switch (kind)
		{
			case LOAD:		    return new String[] { jreExecName, jreArgsName, chromeDriverPathName, geckoDriverPathName, ieDriverPathName, 
			        chromeDriverBinary, firefoxProfileDir, usePrivateMode, logLevel };
			case START:         return new String[] { browserName, urlName };
			case GET_PROPERTY:  return new String[] { propertyUrlName, propertyTitle, propertyAllTitles, propertyCookie, propertyAllCookies };
            case SET_PROPERTY:  return new String[] { propertyUrlName, propertyTitle, propertyAddCookie, propertyRemoveCookie, propertyRemoveAllCookies };
			case NEW_INSTANCE:  return new String[] { urlName, whereOpenName };
			default:		    return empty;	
		}
	}

	@Override
	public boolean canFillParameter(String parameterToFill)
	{
		return browserName.equals(parameterToFill)
			|| whereOpenName.equals(parameterToFill);
	}

	@Override
	public String[] listForParameter(String parameterToFill)
	{
		switch (parameterToFill)
		{
			case browserName:
				return new String[]
						{
							"AndroidChrome",
							"AndroidBrowser",
							"Firefox",
							"Chrome",
							"InternetExplorer",
							"Opera",
							"PhantomJS",
							"Safari",
						};

			case whereOpenName:
				return WhereToOpen.names();

			default:
				return new String[0];
		}
	}

	//----------------------------------------------------------------------------------------------
	// IApplicationFactory
	//----------------------------------------------------------------------------------------------
	@Override
	public void init(IGuiDictionary dictionary)
	{
		this.dictionary = dictionary;
	}

    @Override
    public InputStream getHelp()
    {
        return WebAppFactory.class.getResourceAsStream(helpFileName);
    }

    @Override
    public Set<ControlKind> supportedControlKinds()
    {
        return Arrays.stream(supportedControls)
                .sorted((c1,c2) -> c1.name().compareTo(c2.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

	@Override
	public IApplication createApplication()
	{
		return new ProxyWebApp();
	}

	@Override
	public String getRemoteClassName()
	{
		return SeleniumRemoteApplication.class.getCanonicalName();
	}

	@Override
	public IGuiDictionary getDictionary()
	{
		return this.dictionary;
	}

	@Override
	public boolean isAllowed(ControlKind kind, OperationKind operation) {
		return getInfo().isAllowed(kind, operation);
	}

	@Override
	public boolean isSupported(ControlKind kind) {
		return getInfo().isSupported(kind);
	}

	@Override
    public PluginInfo getInfo()
    {
        Map<LocatorFieldKind, String>   fieldMap = new HashMap<>();
        
        fieldMap.put(LocatorFieldKind.ACTION,       "action");
        fieldMap.put(LocatorFieldKind.UID,          "id"); 
        fieldMap.put(LocatorFieldKind.CLAZZ,        "class");
        fieldMap.put(LocatorFieldKind.NAME,         "name");
        fieldMap.put(LocatorFieldKind.TITLE,        "title");
        fieldMap.put(LocatorFieldKind.TEXT,         "placeholder");
        fieldMap.put(LocatorFieldKind.TOOLTIP,      "title");

		PluginInfo info = new PluginInfo(fieldMap);

		info.addTypes(ControlKind.Any, "*");
        info.addTypes(ControlKind.Button, "button", "input", "a", "img");
        info.addTypes(ControlKind.CheckBox, "button", "input");
		info.addTypes(ControlKind.ComboBox,"select", "input");
		info.addTypes(ControlKind.Dialog,"form");
		info.addTypes(ControlKind.Frame,"form", "body", "frame", "iframe");
		info.addTypes(ControlKind.Image,"img");
		info.addTypes(ControlKind.Label,"label", "span");
		info.addTypes(ControlKind.Panel,"div");
		info.addTypes(ControlKind.ProgressBar,"progress");
		info.addTypes(ControlKind.RadioButton,"input");
		info.addTypes(ControlKind.Row,"tr");
		info.addTypes(ControlKind.Slider,"div");
		info.addTypes(ControlKind.Spinner,"*");
        info.addTypes(ControlKind.Table,"table");
        //info.addTypes(ControlKind.TabPanel,"button");
        info.addTypes(ControlKind.TextBox,"input", "textarea");
		info.addTypes(ControlKind.ToggleButton,"input");
		info.addTypes(ControlKind.ListView,"ul");
        info.addTypes(ControlKind.Wait,"*");

		return info;
    }
}
