////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import com.exactprosystems.jf.api.app.ControlKind;
import com.exactprosystems.jf.api.app.Locator;
import com.exactprosystems.jf.api.app.Visibility;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class MatcherSelenium extends By
{
	public MatcherSelenium(ControlKind controlKind, Locator locator) throws RemoteException
	{
		if (locator == null)
		{
			throw new NullPointerException("locator");
		}

		this.xpath = xpathFromControl(controlKind, locator);
		this.visibility = locator.getVisibility();

		logger.debug("=========================================");
		logger.debug("Matcher control = " + controlKind);
		logger.debug("Matcher locator = " + locator);
		logger.debug("Matcher xpath   = " + xpath);
	}

	@Override
	public List<WebElement> findElements(SearchContext context)
	{
		logger.debug("-----------------------------------------");
		List<WebElement> found = context.findElements(By.xpath(this.xpath));
		int count = 0;
		int countEnabledElement = 0;
		List<WebElement> result = new ArrayList<WebElement>();
		if (found != null)
		{
			for(WebElement element : found)
			{
				if (element != null)
				{
					if (element.isDisplayed())
					{
						logger.debug("Found element : " + SeleniumRemoteApplication.getElementString(element));
						result.add(element);
					}
					else if (visibility != null)
					{
						if (visibility == Visibility.Visible)
						{
							result.add(element);
						}
					}
					if (element.isEnabled())
					{
						countEnabledElement++;
					}
				}
				else
				{
					count++;
				}
			}
		}

		logger.debug("Matcher found " + result.size() + " elements. Not displayed " + count + " elements. Enabled element : " + countEnabledElement);

		return result;
	}

	private String xpathFromControl(ControlKind controlKind, Locator locator)
	{
		logger.debug("locator use absolute xpath : " + locator.useAbsoluteXpath());
		if (locator.getXpath() != null)
		{
			//TODO this is is Deprecated
			if (locator.getXpath().startsWith("~"))
			{
				return locator.getXpath().substring(1);
			}
			if (locator.useAbsoluteXpath())
			{
				return locator.getXpath();
			}
		}
		if (controlKind == null)
		{
			return null;
		}
		switch (controlKind)
		{
			case Any:			return complexXpath(locator, "*");
			case Button: 		return complexXpath(locator, "button", "input", "a", "img");
			case CheckBox: 		return complexXpath(locator, "button", "input");
			case ComboBox: 		return complexXpath(locator, "select", "input");
			case Dialog: 		return complexXpath(locator, "form");
			case Frame: 		return complexXpath(locator, "form", "body");
			case Label: 		return complexXpath(locator, "label");
			case MenuItem: 		return complexXpath(locator, "li");
			case Panel: 		return complexXpath(locator, "div");
			case RadioButton: 	return complexXpath(locator, "input");
			case Row: 			return complexXpath(locator, "tr");
			case Table: 		return complexXpath(locator, "table");
			case TabPanel: 		return complexXpath(locator, "button");
			case TextBox: 		return complexXpath(locator, "input", "textarea");
			case ToggleButton: 	return complexXpath(locator, "input");
			case ListView: 		return complexXpath(locator, "ul");
			case Tree: 			return null;
			case Wait: 			return complexXpath(locator, "*");
            case Tooltip:       return complexXpath(locator, "*");
			case Menu:			return complexXpath(locator, "li");
			case TreeItem:		return null;
		}
		return complexXpath(locator, "*");
	}

	private String complexXpath(Locator locator, String ... strings)
	{
		if (strings == null || strings.length == 0)
		{
			return null;
		}

		String separator = "";
		StringBuilder sb = new StringBuilder();
		for (String str : strings)
		{
			String filter = filterForLocator(locator);

			sb.append(separator);
			sb.append(String.format(".//%s[%s]", str, filter));
			separator = " | ";
		}

		return sb.toString();
	}

	public String filterForLocator(Locator locator)
	{
		StringBuilder sb = new StringBuilder();
		String separator = "";

		if (locator.getUid() != null)
		{
			sb.append(separator); separator = " and ";
			sb.append(String.format("@id='%s'", locator.getUid()));
		}
		if (locator.getXpath() != null)
		{
			sb.append(separator); separator = " and ";
			sb.append(String.format("(%s)", locator.getXpath()));
		}
		if (locator.getClazz() != null)
		{
			for (String part : locator.getClazz().split(" "))
			{
				sb.append(separator); separator = " and ";

				if (part.startsWith("!"))
				{
					sb.append(String.format("not (contains(@class, '%s'))", part.substring(1)));
				}
				else
				{
					sb.append(String.format("contains(@class, '%s')", part));
				}
			}
		}
		if (locator.getName() != null)
		{
			sb.append(separator); separator = " and ";
			sb.append(String.format("@name='%s'", locator.getName()));
		}
		if (locator.getTitle() != null)
		{
			sb.append(separator); separator = " and ";
			sb.append(String.format("contains(@title,'%s')", locator.getTitle()));
		}
		if (locator.getAction() != null)
		{
			sb.append(separator); separator = " and ";
			sb.append(String.format("@action='%s'", locator.getAction()));
		}
		if (locator.getText() != null)
		{
			sb.append(separator); separator = " and ";
			if (locator.isWeak())
			{
				sb.append(String.format("contains(.,'%s') or contains(@placeholder,'%s')", locator.getText(), locator.getText()));
			}
			else
			{
				sb.append(String.format("(.='%s') or @placeholder='%s'", locator.getText(), locator.getText()));
			}
		}
		if (locator.getTooltip() != null)
		{
			sb.append(separator);
			sb.append(String.format("@title='%s'", locator.getTooltip()));
		}
		return sb.length() == 0 ? "*" : sb.toString();
	}


	private String xpath;
	private Visibility visibility;
	public static void setLogger(Logger log)
	{
		logger = log;
	}

	protected static Logger logger = null;

}
