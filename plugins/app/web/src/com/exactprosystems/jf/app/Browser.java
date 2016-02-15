////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.io.File;

public enum Browser
{
	FIREFOX				("Firefox"),
	CHROME				("Chrome"),
	INTERNETEXPLORER	("InternetExplorer"),
	OPERA				("Opera"),
	PHANTOMJS			("PhantomJS"),
	SAFARI				("Safari");

	private String browserName;

	Browser(String name)
	{
		this.browserName = name;
	}

	public String getBrowserName()
	{
		return browserName;
	}

	public WebDriver createDriver(String pathToBinary) throws Exception
	{
		switch (this)
		{
			case FIREFOX:
				return new FirefoxDriver();

			case CHROME:
				if (pathToBinary != null && !pathToBinary.isEmpty())
				{
					ChromeOptions options = new ChromeOptions();
					options.setBinary(new File(pathToBinary));
					return new ChromeDriver(options);
				}
				return new ChromeDriver();

			case INTERNETEXPLORER:
				return new InternetExplorerDriver();

			case OPERA:
				return new OperaDriver();

			case PHANTOMJS:
				return new PhantomJSDriver();

			case SAFARI:
				return new SafariDriver();

			default:
				throw new Exception("Unknown browser : " + this.browserName);
		}

	}
}
