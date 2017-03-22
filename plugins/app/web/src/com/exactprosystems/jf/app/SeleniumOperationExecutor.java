////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.app;

import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.api.client.ICondition;
import com.exactprosystems.jf.api.common.Converter;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.error.app.ElementNotFoundException;
import com.exactprosystems.jf.api.error.app.TooManyElementsException;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.Select;

import java.awt.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

public class SeleniumOperationExecutor implements OperationExecutor<WebElement>
{
	private static final String tag_tbody 	= "tbody";
	private static final String tag_tr 		= "tr";
	private static final String tag_td 		= "td";
	private static final String tag_thead 	= "thead";
	private static final String tag_th 		= "th";
	private static final String css_prefix	= "css:";
	private static final String row_span	= "rowspan";
	private static final String col_span	= "colspan";


	private final int repeatLimit = 4;

	public SeleniumOperationExecutor(WebDriverListenerNew driver, Logger logger)
	{
		this.driver = driver;
		this.logger = logger;
		this.customAction = new Actions(this.driver);
	}

    @Override
    public void setPluginInfo(PluginInfo info)
    {
        this.info = info;
    }

	@Override
	public Rectangle getRectangle(WebElement component) throws Exception
	{
		try
		{
			Point location = component.getLocation();
			Dimension size = component.getSize();
			return new Rectangle(location.getX(), location.getY(), size.getWidth(), size.getHeight());
		}
		catch (Throwable e)
		{
			logger.error(String.format("getRectangle(%s)", component));
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public Color getColor(String color) throws Exception
	{
		if (color == null)
		{
			return null;
		}

		if (color.equalsIgnoreCase("transparent"))
		{
			return new Color(255, 255, 255, 0);
		}
		StringBuilder colorSB = new StringBuilder(color);
		colorSB.delete(0, 5);
		colorSB.deleteCharAt(colorSB.length() - 1);
		String[] colors = colorSB.toString().split(", ");
		return new Color(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]), Integer.parseInt(colors[3]));
	}

	//region table is container
	@Override
	public boolean tableIsContainer()
	{
		return true;
	}

	@Override
	public boolean mouseTable(WebElement component, int column, int row, MouseAction action) throws Exception
	{
		return false;    // the realization is not needed because table is a container
	}

	@Override
	public String getValueTableCell(WebElement component, int column, int row) throws Exception
	{
		return null;    // the realization is not needed because table is a container
	}

	@Override
	public boolean textTableCell(WebElement component, int column, int row, String text) throws Exception
	{
		return false;    // the realization is not needed because table is a container 
	}
	//endregion

	@Override
	public String get(WebElement component) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				if (component.getTagName().equals("input") || component.getTagName().equals("select"))
				{
					return component.getAttribute("value");
				}
				return component.getText();
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public String getAttr(WebElement component, String name) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				if (name == null)
				{
					return null;
				}
				if (name.startsWith(css_prefix))
				{
					String cssAttrName = name.substring(css_prefix.length());
					return component.getCssValue(cssAttrName);
				}
				return component.getAttribute(name);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public String script(WebElement component, String script) throws Exception
	{
		int repeat = 1;
		Exception real = null;
		do
		{
			try
			{
				Object ret = driver.executeScript(script, component);
				return String.valueOf(ret);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error(String.format("Error script(%s, %s)", component, script));
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean dragNdrop(WebElement drag, int x1, int y1, WebElement drop, int x2, int y2, boolean moveCursor) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				Actions actions = new Actions(this.driver).clickAndHold(drag);
				if (drop == null)
				{
					actions.moveByOffset(x2, y2);
				}
				else
				{
					actions.moveToElement(drop, x2, y2);
				}
				actions.release().perform();
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error(e.getMessage(), e);
				throw new RemoteException("Error on drag and drop");
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	//region public table methods
	/*
	we need parse another tables
<div>
	<span> thead tr th
	<table id="t1">
		<thead>
			<tr>
				<th>th1</th>
				<th>th2</th>
				<th>th3</th>
			</tr>
		</thead>
	
		<tbody>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
<div>
	<span>thead tr td
	<table id="t2">
		<thead>
			<tr>
				<td>th1</td>
				<td>th2</td>
				<td>th3</td>
			</tr>
		</thead>
	
		<tbody>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
	
<div>
	<span> thead th
	<table id="t3">
		<thead>
			<th>th1</th>
			<th>th2</th>
			<th>th3</th>
		</thead>
	
		<tbody>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
	
<div>
	<span> thead td
	<table id="t4">
		<thead>
			<td>th1</td>
			<td>th2</td>
			<td>th3</td>
		</thead>
	
		<tbody>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
	
<div>
	<span> tr th
	<table id="t5">
		<tbody>
			<tr>
				<th>th1</th>
				<th>th2</th>
				<th>th3</th>
			</tr>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
<div>
	<span> tr td
	<table id="t6">
		<tbody>
			<tr>
				<td>th1</td>
				<td>th2</td>
				<td>th3</td>
			</tr>
			<tr>
				<td>td1</td>
				<td>td2</td>
				<td>td3</td>
			</tr>
		</tbody>
	</table>
</div>
	 */
	@Override
	public Map<String, String> getRow(WebElement table, Locator additional, Locator header, boolean useNumericHeader, String[] columns, ICondition valueCondition, ICondition colorCondition) throws Exception
	{
		int repeat = 1;
		Exception real = null;
		do
		{
			try
			{
				List<Map<String, String>> list = new ArrayList<>();
				List<String> headers = null;
				if(header != null)
				{
					headers = getHeadersFromHeaderField(header);
				}
				else
				{
					headers = getHeaders(table, columns);
				}

				List<WebElement> rows = findRows(additional, table);

				for (WebElement row : rows)
				{
					if (rowMatches(row, valueCondition, colorCondition, headers))
					{
						list.add(getRowValues(row, headers));
					}
				}
				if (list.size() == 1)
				{
					return list.get(0);
				}

				throw new RemoteException("Found " + list.size() + " rows instead 1.");
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error(String.format("Error getRow(%s, %s, %s)", table, valueCondition, colorCondition));
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public List<String> getRowIndexes(WebElement table, Locator additional, Locator header, boolean useNumericHeader, String[] columns, ICondition valueCondition, ICondition colorCondition) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<String> result = new ArrayList<>();
				List<String> headers = null;
				if(header != null)
				{
					headers = getHeadersFromHeaderField(header);
				}
				else
				{
					headers = getHeaders(table, columns);
				}

				List<WebElement> rows = findRows(additional, table);
				for (int i = 0; i < rows.size(); i++)
				{
					WebElement row = rows.get(i);
					if (rowMatches(row, valueCondition, colorCondition, headers))
					{
						result.add(String.valueOf(i));
					}
				}
				return result;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error(String.format("Error getRowIndexes(%s, %s, %s)", table, valueCondition, colorCondition));
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public Map<String, String> getRowByIndex(WebElement table, Locator additional, Locator header, boolean useNumericHeader, String[] columns, int i) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<String> headers = null;
				if(header != null)
				{
					headers = getHeadersFromHeaderField(header);
				}
				else
				{
					headers = getHeaders(table, columns);
				}

				this.logger.debug("Found headers : " + headers);
				List<WebElement> rows = findRows(additional, table);
				this.logger.debug("Found rows. Rows size : " + rows.size());
				if (i > rows.size() - 1 || i < 0)
				{
					throw new RemoteException("Invalid index : " + i + ", max index : " + (rows.size() - 1));
				}
				this.logger.debug("rows.get(i).getText() : " + rows.get(i).getText());
				return getRowValues(rows.get(i), headers);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error("Error on get row by index");
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public Map<String, ValueAndColor> getRowWithColor(WebElement component, Locator additional, Locator header, boolean useNumericHeader, String[] columns, int i) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<String> headers = null;
				if(header != null)
				{
					headers = getHeadersFromHeaderField(header);
				}
				else
				{
					headers = getHeaders(component, columns);
				}

				List<WebElement> rows = findRows(additional, component);

				if (rows.isEmpty())
				{
					logger.error("Table is empty");
					throw new RemoteException("Table is empty");
				}

				if (i < 0 || i > rows.size() - 1)
				{
					throw new RemoteException("Invalid index=[" + i + "]. Maximum index=[" + (rows.size() - 1) + "].");
				}
				return valueFromRow(rows.get(i), headers);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error("Error on get row with color");
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public String[][] getTable(WebElement component, Locator additional, Locator header, boolean useNumericHeader, String[] columns) throws Exception
	{
		String outerHTML = component.getAttribute("outerHTML");
		Document doc = Jsoup.parse(outerHTML);
		AtomicBoolean ab = new AtomicBoolean(false);
		List<String> headers = null;
		if(header != null)
		{
			headers = getHeadersFromHeaderField(header);
		}
		else
		{
			headers = getHeadersFromHTML(outerHTML, ab, columns);
		}
		logger.debug("Headers : " + headers);
		Elements rows = findRows(doc);
		if (ab.get())
		{
			rows.remove(0);
		}
		logger.debug("Rows size : " + rows.size());
		String[][] res = new String[rows.size() + 1][headers.size()];
		String[] cols = res[0];
		for (int i = 0; i < cols.length; i++)
		{
			cols[i] = headers.get(i);
		}
		for (int i = 1; i < res.length; i++)
		{
			Element row = rows.get(i - 1);
			Elements cells = row.children();
			for (int j = 0; j < Math.min(cols.length, cells.size()); j++)
			{
				res[i][j] = cells.get(j).text();
			}
		}
		logger.debug("Result table");
		for (String[] re : res)
		{
			logger.debug(Arrays.toString(re));
		}
		logger.debug("############");
		return res;
	}

	private List<String> getHeadersFromHeaderField(Locator header) throws Exception {
		WebElement headerTable = find(null, header);
		List<WebElement> thRows = headerTable.findElements(By.tagName(tag_th));
		if (!thRows.isEmpty())
		{
			return Converter.convertColumns(convertColumnsToHeaders(thRows, null, new WebElementText()));
		}
		List<WebElement> tdRows = headerTable.findElements(By.tagName(tag_td));
		if (!tdRows.isEmpty())
		{
			return Converter.convertColumns(convertColumnsToHeaders(tdRows, null, new WebElementText()));
		}
		return null;
	}

	@Override
	public int getTableSize(WebElement component, Locator additional, Locator header, boolean useNumericHeader) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<WebElement> tbody = component.findElements(By.xpath("child::tbody"));
				if (tbody.isEmpty())
				{
					return 0;
				}
				WebElement firstTbody = tbody.get(0);
				return firstTbody.findElements(By.xpath("child::tr")).size();
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error("Error on get row with color");
				logger.error(e.getMessage(), e);
				throw new RemoteException(e.getMessage());
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}
	
    @Override
    public Color getColorXY(WebElement component, int x, int y) throws Exception
    {
        Point location = component.getLocation();
        File image = driver.getScreenshotAs(OutputType.FILE);
        BufferedImage bufferedImage = ImageIO.read(image);
        int rgb = bufferedImage.getRGB(location.x + x, location.y + y);
        return new Color(rgb);
    }
	//endregion

	//region public find methods
	@Override
	public List<WebElement> findAll(ControlKind controlKind, WebElement window, Locator locator) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				By by = new MatcherSelenium(this.info, controlKind, locator);
				return (window == null) ? driver.findElements(by) : window.findElements(by);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public List<WebElement> findAll(Locator owner, Locator locator) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				WebElement window = null;
				if (owner != null)
				{
					List<WebElement> elements = findAll(owner.getControlKind(), null, owner);

					if (elements.isEmpty())
					{
						throw new ElementNotFoundException("Owner", owner);
					}

					if (elements.size() > 1)
					{
						throw new TooManyElementsException("" + elements.size(),owner);
					}
					window = elements.get(0);
				}
				By by = new MatcherSelenium(this.info, locator.getControlKind(), locator);
				return (window == null) ? driver.findElements(by) : window.findElements(by);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public WebElement find(Locator owner, Locator locator) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				WebElement window = null;

				if (owner != null)
				{
					List<WebElement> elements = findAll(owner.getControlKind(), null, owner);

					if (elements.isEmpty())
					{
						throw new ElementNotFoundException("Owner", owner);
					}

					if (elements.size() > 1)
					{
						throw new TooManyElementsException("" + elements.size(), owner);
					}
					window = elements.get(0);
				}

				ControlKind controlKind = locator.getControlKind();

				List<WebElement> elements = findAll(controlKind, window, locator);

				if (elements.isEmpty())
				{
					if (locator.isWeak())
					{
						return new DummyWebElement();
					}
					else
					{
						throw new ElementNotFoundException(locator);
					}
				}

				if (elements.size() > 1)
				{
					for (WebElement element : elements)
					{
						logger.error("Found : " + getElementString(element));
					}
					throw new TooManyElementsException("" + elements.size(), locator);
				}
				return elements.get(0);
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public WebElement lookAtTable(WebElement tableComp, Locator additional, Locator header, int x, int y) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			logger.info("findIntoTable(" + SeleniumRemoteApplication.getElementString(tableComp) + ", " + x + ", " + y + ")" + (additional == null ? "" : "additional " + additional));
			try
			{
				if (y < 0)
				{
					List<WebElement> headers = getHeaders(tableComp);
					return headers.get(x);
				}
				else if (x < 0)
				{
					List<WebElement> rows = findRows(additional, tableComp);
					return rows.get(y);
				}
				else
				{
					getHeaders(tableComp, null);
					List<WebElement> rows = findRows(additional, tableComp);
					WebElement row1 = rows.get(y);
					List<WebElement> cells1 = row1.findElements(By.xpath("child::" + tag_td));
					return cells1.get(x);
				}
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
			catch (Exception e)
			{
				logger.error(e.getMessage(), e);
				throw new RemoteException("Error on find into table");
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean elementIsEnabled(WebElement component)
	{
		return component.isEnabled();
	}
	//endregion

	@Override
	public boolean mouse(WebElement component, int x, int y, MouseAction action) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				if (component instanceof DummyWebElement) 
				{
					return true;
				}
				
				scrollToElement(component);
				switch (action)
				{
					case Move:
						if (this.driver.getWrappedDriver() instanceof SafariDriver)
						{
							throw new RemoteException("Don't support");
						}
						if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
						{
							customAction.moveToElement(component).perform();
						}
						else
						{
							customAction.moveToElement(component, x, y).perform();
						}
						break;

					case LeftClick:
						if (this.driver.getWrappedDriver() instanceof SafariDriver)
						{
							component.click();
						}
						else
						{
							/* //reserve code in case of flicker and double clicking in the IE
							if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
							{
								if(driver.getWrappedDriver() instanceof InternetExplorerDriver)
								{
									clickByJavascript(component);
								}
								else customAction.moveToElement(component).click().perform();
							}
							else
							{
								if(driver.getWrappedDriver() instanceof InternetExplorerDriver)
								{
									clickByJavascriptByXY(component, x, y);
								}
								else customAction.moveToElement(component, x, y).click().perform();
							}*/
							
							if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
							{
								customAction.moveToElement(component).click().perform();
							}
							else
							{
								customAction.moveToElement(component, x, y).click().perform();
							}
						}
						break;

					case LeftDoubleClick:
						if (this.driver.getWrappedDriver() instanceof SafariDriver)
						{
							throw new RemoteException("Don't support");
						}
						if (driver.getWrappedDriver() instanceof ChromeDriver)
						{
							driver.executeScript("var evt = document.createEvent('MouseEvents');" + "evt.initMouseEvent('dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);" + "arguments[0].dispatchEvent(evt);", component);
						}
						else
						{
							if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
							{
								customAction.moveToElement(component).doubleClick().perform();
							}
							else
							{
								customAction.moveToElement(component, x, y).doubleClick().perform();
							}
						}
						break;

					case RightClick:
						if (this.driver.getWrappedDriver() instanceof SafariDriver)
						{
							throw new RemoteException("Don't support");
						}
						if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
						{
							customAction.contextClick(component).perform();
						}
						else
						{
							customAction.moveToElement(component, x, y).contextClick().perform();
						}
						break;

					case RightDoubleClick:
						return false;
				}
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean push(WebElement component) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				/* //reserve code in case of flicker and double clicking in the IE
				if(driver.getWrappedDriver() instanceof InternetExplorerDriver)	clickByJavascript(component); 
					else component.click();
				*/
				component.click();
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean text(WebElement component, String text, boolean clear) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				if (clear)
				{
					component.clear();
				}
				component.sendKeys(text);
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean toggle(WebElement component, boolean value) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				boolean isNormalCheckBox = component.getTagName().equals("input") && Str.areEqual(component.getAttribute("type"), "checkbox");
				scrollToElement(component);
				if (isNormalCheckBox)
				{
					if (value ^ Str.areEqual(component.getAttribute("checked"), "true"))
					{
						component.click();
					}
				}
				else
				{
					component.click();
				}
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean selectByIndex(WebElement component, int index) throws Exception
	{
		scrollToElement(component);
		new Select(component).selectByIndex(index);
		return true;
	}

	@Override
	public boolean select(WebElement component, String selectedText) throws Exception
	{
		//TODO think about it
		scrollToElement(component);
		new Select(component).selectByVisibleText(selectedText);
		return true;
	}

	@Override
	public boolean expand(WebElement component, String path, boolean expandOrCollapse) throws Exception
	{
		// TODO process the parameter path
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				component.click();
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public boolean wait(Locator locator, int ms, boolean toAppear, AtomicLong atomicLong) throws Exception
	{
		long begin = System.currentTimeMillis();
		try
		{
			Exception real = null;
			int repeat = 1;
			do
			{
				try
				{
					logger.debug("Wait to " + (toAppear ? "" : "Dis") + "appear for " + locator + " on time " + ms);
					final By by = new MatcherSelenium(this.info, locator.getControlKind(), locator);

					long time = System.currentTimeMillis();
					while (System.currentTimeMillis() < time + ms)
					{
						try
						{
							List<WebElement> elements = driver.findElements(by);
							if (toAppear)
							{
								if (elements.size() > 0)
								{
									return true;
								}
							}
							else
							{
								if (elements.size() == 0)
								{
									return true;
								}
							}
						}
						catch (Exception e)
						{
							logger.error("Error on waiting");
							logger.error(e.getMessage(), e);
						}
					}
					return false;
				}
				catch (StaleElementReferenceException e)
				{
					real = e;
					logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
				}
			}
			while (++repeat < repeatLimit);
			throw real;
		}
		finally
		{
			if (atomicLong != null)
			{
				atomicLong.set(System.currentTimeMillis() - begin);
			}
		}
	}

	@Override
	public boolean press(WebElement component, Keyboard key) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				switch (key)
				{
					case ESCAPE:
						this.customAction.sendKeys(Keys.ESCAPE).perform();
						break;
					case F1:
						this.customAction.sendKeys(Keys.F1).perform();
						break;
					case F2:
						this.customAction.sendKeys(Keys.F2).perform();
						break;
					case F3:
						this.customAction.sendKeys(Keys.F3).perform();
						break;
					case F4:
						this.customAction.sendKeys(Keys.F4).perform();
						break;
					case F5:
						this.customAction.sendKeys(Keys.F5).perform();
						break;
					case F6:
						this.customAction.sendKeys(Keys.F6).perform();
						break;
					case F7:
						this.customAction.sendKeys(Keys.F7).perform();
						break;
					case F8:
						this.customAction.sendKeys(Keys.F8).perform();
						break;
					case F9:
						this.customAction.sendKeys(Keys.F9).perform();
						break;
					case F10:
						this.customAction.sendKeys(Keys.F10).perform();
						break;
					case F11:
						this.customAction.sendKeys(Keys.F11).perform();
						break;
					case F12:
						this.customAction.sendKeys(Keys.F12).perform();
						break;

					case DIG1:
						this.customAction.sendKeys(Keys.NUMPAD1).perform();
						break;
					case DIG2:
						this.customAction.sendKeys(Keys.NUMPAD2).perform();
						break;
					case DIG3:
						this.customAction.sendKeys(Keys.NUMPAD3).perform();
						break;
					case DIG4:
						this.customAction.sendKeys(Keys.NUMPAD4).perform();
						break;
					case DIG5:
						this.customAction.sendKeys(Keys.NUMPAD5).perform();
						break;
					case DIG6:
						this.customAction.sendKeys(Keys.NUMPAD6).perform();
						break;
					case DIG7:
						this.customAction.sendKeys(Keys.NUMPAD7).perform();
						break;
					case DIG8:
						this.customAction.sendKeys(Keys.NUMPAD8).perform();
						break;
					case DIG9:
						this.customAction.sendKeys(Keys.NUMPAD9).perform();
						break;
					case DIG0:
						this.customAction.sendKeys(Keys.NUMPAD0).perform();
						break;
					case BACK_SPACE:
						this.customAction.sendKeys(Keys.BACK_SPACE).perform();
						break;
					case INSERT:
						this.customAction.sendKeys(Keys.INSERT).perform();
						break;
					case HOME:
						this.customAction.sendKeys(Keys.HOME).perform();
						break;
					case PAGE_UP:
						this.customAction.sendKeys(Keys.PAGE_UP).perform();
						break;

					case TAB:
						this.customAction.sendKeys(Keys.TAB).perform();
						break;
					case Q:
						this.customAction.sendKeys("q").perform();
						break;
					case W:
						this.customAction.sendKeys("w").perform();
						break;
					case E:
						this.customAction.sendKeys("e").perform();
						break;
					case R:
						this.customAction.sendKeys("r").perform();
						break;
					case T:
						this.customAction.sendKeys("t").perform();
						break;
					case Y:
						this.customAction.sendKeys("y").perform();
						break;
					case U:
						this.customAction.sendKeys("u").perform();
						break;
					case I:
						this.customAction.sendKeys("i").perform();
						break;
					case O:
						this.customAction.sendKeys("o").perform();
						break;
					case P:
						this.customAction.sendKeys("p").perform();
						break;
					case SLASH:
						break;
					case BACK_SLASH:
						break;
					case DELETE:
						this.customAction.sendKeys(Keys.DELETE).perform();
						break;
					case END:
						this.customAction.sendKeys(Keys.END).perform();
						break;
					case PAGE_DOWN:
						this.customAction.sendKeys(Keys.PAGE_DOWN).perform();
						break;

					case CAPS_LOCK:
						break;
					case A:
						this.customAction.sendKeys("a").perform();
						break;
					case S:
						this.customAction.sendKeys("s").perform();
						break;
					case D:
						this.customAction.sendKeys("d").perform();
						break;
					case F:
						this.customAction.sendKeys("f").perform();
						break;
					case G:
						this.customAction.sendKeys("g").perform();
						break;
					case H:
						this.customAction.sendKeys("h").perform();
						break;
					case J:
						this.customAction.sendKeys("j").perform();
						break;
					case K:
						this.customAction.sendKeys("k").perform();
						break;
					case L:
						this.customAction.sendKeys("l").perform();
						break;
					case SEMICOLON:
						this.customAction.sendKeys(Keys.SEMICOLON).perform();
						break;
					case QUOTE:
						break;
					case DOUBLE_QUOTE:
						break;
					case ENTER:
						this.customAction.sendKeys(Keys.ENTER).perform();
						break;

					case SHIFT:
						this.customAction.sendKeys(Keys.SHIFT).perform();
						break;
					case Z:
						this.customAction.sendKeys("z").perform();
						break;
					case X:
						this.customAction.sendKeys("x").perform();
						break;
					case C:
						this.customAction.sendKeys("c").perform();
						break;
					case V:
						this.customAction.sendKeys("v").perform();
						break;
					case B:
						this.customAction.sendKeys("b").perform();
						break;
					case N:
						this.customAction.sendKeys("n").perform();
						break;
					case M:
						this.customAction.sendKeys("m").perform();
						break;
					case DOT:
						this.customAction.sendKeys(Keys.DECIMAL).perform();
						break;
					case UP:
						this.customAction.sendKeys(Keys.UP).perform();
						break;

					case CONTROL:
						this.customAction.sendKeys(Keys.CONTROL).perform();
						break;
					case ALT:
						this.customAction.sendKeys(Keys.ALT).perform();
						break;
					case SPACE:
						this.customAction.sendKeys(Keys.SPACE).perform();
						break;
					case LEFT:
						this.customAction.sendKeys(Keys.LEFT).perform();
						break;
					case DOWN:
						this.customAction.sendKeys(Keys.DOWN).perform();
						break;

					case RIGHT:
						this.customAction.sendKeys(Keys.RIGHT).perform();
						break;

					case PLUS:
						this.customAction.sendKeys(Keys.ADD).perform();
						break;
					case MINUS:
						this.customAction.sendKeys(Keys.SUBTRACT).perform();
						break;
					case UNDERSCORE:
						this.customAction.sendKeys(Keys.chord(Keys.SHIFT, "-")).perform();
						break;

					case NUM_LOCK:
						//todo nothing, because Keys dosen't contains numlock key
						break;
					case NUM_DIVIDE:
						this.customAction.sendKeys(Keys.DIVIDE).perform();
						break;
					case NUM_SEPARATOR:
						this.customAction.sendKeys(Keys.SEPARATOR).perform();
						break;
					case NUM_MULTIPLY:
						this.customAction.sendKeys(Keys.MULTIPLY).perform();
						break;
					case NUM_MINUS:
						this.customAction.sendKeys(Keys.SUBTRACT).perform();
						break;
					case NUM_DIG7:
						this.customAction.sendKeys(Keys.NUMPAD7).perform();
						break;
					case NUM_DIG8:
						this.customAction.sendKeys(Keys.NUMPAD8).perform();
						break;
					case NUM_DIG9:
						this.customAction.sendKeys(Keys.NUMPAD9).perform();
						break;
					case NUM_PLUS:
						this.customAction.sendKeys(Keys.ADD).perform();
						break;
					case NUM_DIG4:
						this.customAction.sendKeys(Keys.NUMPAD4).perform();
						break;
					case NUM_DIG5:
						this.customAction.sendKeys(Keys.NUMPAD5).perform();
						break;
					case NUM_DIG6:
						this.customAction.sendKeys(Keys.NUMPAD6).perform();
						break;
					case NUM_DIG1:
						this.customAction.sendKeys(Keys.NUMPAD1).perform();
						break;
					case NUM_DIG2:
						this.customAction.sendKeys(Keys.NUMPAD2).perform();
						break;
					case NUM_DIG3:
						this.customAction.sendKeys(Keys.NUMPAD3).perform();
						break;
					case NUM_DIG0:
						this.customAction.sendKeys(Keys.NUMPAD0).perform();
						break;
					case NUM_DOT:
						this.customAction.sendKeys(Keys.DECIMAL).perform();
						break;
					case NUM_ENTER:
						this.customAction.sendKeys(Keys.ENTER).perform();
						break;
					default:
						return false;
				}
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	// MODIFIER_KEYS = new Keys[]{Keys.SHIFT, Keys.CONTROL, Keys.ALT, Keys.META, Keys.COMMAND, Keys.LEFT_ALT, Keys.LEFT_CONTROL, Keys.LEFT_SHIFT};
	// if key not equals modifier keys will throw exception. See org.openqa.selenium.interactions.internal.SingleKeyEvent
	@Override
	public boolean upAndDown(WebElement component, Keyboard key, boolean b) throws Exception
	{
		scrollToElement(component);
		switch (key)
		{
			case SHIFT:
				this.isShiftDown = b;
				if (this.isShiftDown)
				{
					this.customAction.keyDown(Keys.SHIFT);
				}
				else
				{
					this.customAction.keyUp(Keys.SHIFT);
				}
				break;

			case CONTROL:
				this.isCtrlDown = b;
				if (this.isCtrlDown)
				{
					this.customAction.keyDown(Keys.CONTROL);
				}
				else
				{
					this.customAction.keyUp(Keys.CONTROL);
				}
				break;

			case ALT:
				this.isAltDown = b;
				if (this.isAltDown)
				{
					this.customAction.keyDown(Keys.ALT);
				}
				else
				{
					this.customAction.keyUp(Keys.ALT);
				}
				break;

			default:
				return false;
		}
		return true;
	}

	@Override
	public boolean setValue(WebElement component, double value) throws Exception
	{
		if (this.driver.getWrappedDriver() instanceof SafariDriver)
		{
			throw new Exception("Doesn't support");
		}
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				int height = component.getSize().getHeight();
				int width = component.getSize().getWidth();
				if (height > width)
				{
					customAction.moveToElement(component, width / 2, (int) ((double) (value * ((double) height / 100)))).click().build().perform();
				}
				//horizontal slider
				else
				{
					customAction.moveToElement(component, (int) ((double) (value * ((double) width / 100))), height / 2).click().build().perform();
				}

				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	@Override
	public String getValue(WebElement component) throws Exception
	{
		//TODO make this method
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				scrollToElement(component);
				if (component.getTagName().equals("input") || component.getTagName().equals("progress"))
				{
					String attr = component.getAttribute("type");
					if(attr!=null) {
						if (attr.equals("checkbox") || attr.equals("radio")) {
							return String.valueOf(component.isSelected());
						}
					}
					return component.getAttribute("value");
				}
				else if (component.getTagName().equals("select"))
				{
					return new Select(component).getFirstSelectedOption().getText();
				}
				return component.getText();
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	private List<String> getListOfNamesFromListItems(List<WebElement> list)
	{
		ArrayList<String> resultList = new ArrayList<>();
		for (WebElement element : list)
		{
			resultList.add(element.getText());
		}
		return resultList;
	}

	@Override
	public List<String> getList(WebElement component) throws Exception {
		scrollToElement(component);
		switch (component.getTagName())
		{
			case "ul":
			case "ol":
				return getListOfNamesFromListItems(component.findElements(By.xpath("li")));
			case "select":
				return getListOfNamesFromListItems(new Select(component).getOptions());
			default:
				return null;
		}
	}
	
	//region private methods
	private String getElementString(WebElement element)
	{
		String s = element.getAttribute("outerHTML");
		return s.substring(0, s.indexOf(">") + 1);
	}

	private List<String> getHeadersFromHTML(String outerHtml, AtomicBoolean columnsIsRow, String[] columns) throws RemoteException
	{
		Document doc = Jsoup.parse(outerHtml);
		ArrayList<String> result = new ArrayList<>();
		Elements headerElements = null;
		/*
			try to the find element with tag thead.
		 */
		Element lastThead = doc.select(tag_thead).last();
		/*
			if lastThead thead is not present, try to find rows in this table.
		 */
		if (lastThead == null)
		{
			Element firstTr = doc.select(tag_tr).first();
			if (firstTr == null)
			{
				throw new RemoteException("Headers not found. Check your header locator or table locator");
			}
			headerElements = firstTr.children();
			ArrayList<Element> newHeaders = new ArrayList<>();
			for (Element headerElement : headerElements)
			{
				/**
				 * If header has attribute colspan we need add empty columns
				 */
				if (headerElement.hasAttr(col_span))
				{
					String colspan = headerElement.attr(col_span);
					try
					{
						int colSpanInt = Integer.parseInt(colspan);
						for(int i = 0; i < colSpanInt; i++)
						{
							newHeaders.add(null);
						}
					}
					catch (Exception e)
					{
						//nothing do if failing
					}
				}
				else
				{
					newHeaders.add(headerElement);
				}
			}
			if (columns != null && doc.select(tag_th).first() == null )
			{
				columnsIsRow.set(false);
			}else
			{
				columnsIsRow.set(true);
			}
			return Converter.convertColumns(convertColumnsToHeaders(newHeaders, columns, new JsoupElementText()));
		}

		Elements theadChildren = lastThead.children();
		for (Element tr : lastThead.children())
		{
			Elements select = tr.select(tag_th);
			for (Element th : select)
			{
				String s = th.attributes().get(row_span);
				if (!s.isEmpty() && s.equals(String.valueOf(theadChildren.size())))
				{
					result.add(th.text());
				}
			}
		}
		String firstTheadChildTagName = theadChildren.first().tagName();
		Element header = lastThead;
		switch (firstTheadChildTagName)
		{
			case tag_tr: header = theadChildren.last(); break;
		}
		for (Element element : header.children())
		{
//			if (element.hasAttr(col_span))
//			{
//				String attr = element.attr(col_span);
//				try
//				{
//					int colSpanInt = Integer.parseInt(attr);
//					if (colSpanInt == 1)
//					{
//						result.add(element.text());
//					}
//					else
//					{
//						for(int i = 0; i < colSpanInt; i++)
//						{
//							result.add(null);
//						}
//					}
//				}
//				catch (Exception e)
//				{
//					//nothing do if failing
//				}
//			}
//			else
			{
				result.add(element.text());
			}
		}
		return Converter.convertColumns(convertColumnsToHeaders(result, columns, new StringText()));
	}

	private Map<String, String> getRowValues(WebElement row, List<String> headers) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				Map<String, String> result = new LinkedHashMap<>();
				List<WebElement> cells = row.findElements(By.xpath("child::"+tag_td));
				this.logger.debug("Found cells : " + cells.size());

				for (int i = 0; i < headers.size(); i++)
				{
					String key = headers.get(i);
					if (i < cells.size()) {
						WebElement webElement = cells.get(i);
						result.put(key, webElement.getText());
					}
					else
					{
						result.put(key, "");
					}
				}
				return result;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	private List<WebElement> getHeaders(WebElement grid) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<WebElement> theadSections = grid.findElements(By.xpath("child::"+tag_thead));
				if (theadSections.isEmpty())
				{
					return getHeadersFromBody(grid);
				}
				WebElement lastThead = theadSections.get(theadSections.size() - 1);
				List<WebElement> lastTheadElements = lastThead.findElements(By.xpath("child::*"));

				if (lastTheadElements.isEmpty())
				{
					return getHeadersFromBody(grid);
				}
				WebElement webHeader = lastThead;
				String firstChildTagFromLastThead = lastTheadElements.get(0).getTagName();
				switch (firstChildTagFromLastThead)
				{
					case tag_tr: webHeader = lastThead.findElement(By.xpath("child::tr[last()]"));
				}
				return webHeader.findElements(By.xpath("child::*"));
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	private List<String> getHeaders(WebElement grid, String[] columns) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				if (columns != null)
				{
					this.logger.debug("Columns : " + Arrays.toString(columns));
				}

				List<WebElement> theadSections = grid.findElements(By.xpath("child::"+tag_thead)); // find only on child
				if (theadSections.isEmpty())
				{
					return getHeadersFromBody(grid, columns);
				}
				WebElement lastThead = theadSections.get(theadSections.size() - 1);
				List<WebElement> lastTheadElements = lastThead.findElements(By.xpath("child::*"));

				if (lastTheadElements.isEmpty())
				{
					return getHeadersFromBody(grid, columns);
				}

				WebElement webHeader = lastThead;
				String firstChildTagFromLastThead = lastTheadElements.get(0).getTagName();
				switch (firstChildTagFromLastThead)
				{
					case tag_tr: webHeader = lastThead.findElement(By.xpath("child::tr[last()]"));
				}
				List<WebElement> elements = webHeader.findElements(By.xpath("child::*"));
				List<WebElement> newHeaders = new ArrayList<>();
				for (WebElement element : elements)
				{
					String colspan = element.getAttribute(col_span);
					if (!Str.IsNullOrEmpty(colspan))
					{
						try
						{
							int colSpanInt = Integer.parseInt(colspan);
							for(int i = 0; i < colSpanInt; i++)
							{
								newHeaders.add(null);
							}
						}
						catch (Exception e)
						{
							//nothing do if failing
						}
					}
					else
					{
						newHeaders.add(element);
					}
				}
				return Converter.convertColumns(convertColumnsToHeaders(elements, columns, new WebElementText()));
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	interface IText<T>
	{
		String getText(T t);
	}

	private class WebElementText implements IText<WebElement>
	{
		@Override
		public String getText(WebElement webElement)
		{
			return webElement == null ? "" : webElement.getText();
		}
	}

	private class JsoupElementText implements IText<Element>
	{
		@Override
		public String getText(Element element)
		{
			return element == null ? "" : element.text();
		}
	}

	private class StringText implements IText<String>
	{
		@Override
		public String getText(String s)
		{
			return s == null ? "" : s;
		}
	}

	private <T> List<String> convertColumnsToHeaders(Iterable<T> headers, String[] columns, IText<T> func)
	{
		List<String> res = new ArrayList<>();
		if (columns == null)
		{
			for (T header : headers)
			{
				res.add(func.getText(header));
			}
			return res;
		}
		else {
			for (int i = 0; i < columns.length; i++) {
				res.add(columns[i]);
			}
			return res;
		}
	}

	private List<String> getHeadersFromBody(WebElement grid, String[] columns) throws RemoteException
	{
		List<WebElement> rows = grid.findElement(By.xpath("child::" + tag_tbody)).findElements(By.xpath("child::" + tag_tr));
		if (rows.isEmpty())
		{
			throw new RemoteException("Table is empty");
		}
		WebElement firstRow = rows.get(0);
		markRowIsHeader(firstRow, true);
		List<WebElement> cells = firstRow.findElements(By.xpath("child::" + tag_th));;
		if (cells.isEmpty())
		{
			cells = firstRow.findElements(By.xpath("child::" + tag_td));
		}
		ArrayList<WebElement> newHeaders = new ArrayList<>();
		for (WebElement cell : cells)
		{
			String attr = cell.getAttribute(col_span);
			if (!Str.IsNullOrEmpty(attr))
			{
				try
				{
					int colSpanInt = Integer.parseInt(attr);
					for(int i = 0; i < colSpanInt; i++)
					{
						newHeaders.add(null);
					}
				}
				catch (Exception e)
				{
					//nothing do if failing
				}
			}
			else
			{
				newHeaders.add(cell);
			}
		}
		return Converter.convertColumns(convertColumnsToHeaders(newHeaders, columns, new WebElementText()));
	}

	/**
	 * we use current method, if table dosn't contains tag thead and we fill find header from tbody.
	 */
	private List<WebElement> getHeadersFromBody(WebElement grid) throws RemoteException
	{
		List<WebElement> tbodyElements = grid.findElements(By.xpath("child::" + tag_tbody));
		if (tbodyElements.isEmpty())
		{
			throw new RemoteException("Table is empty");
		}

		WebElement firstTbody = tbodyElements.get(0);

		List<WebElement> trs = firstTbody.findElements(By.xpath("child::tr"));
		if (trs.isEmpty())
		{
			throw new RemoteException("Table is empty");
		}

		return trs.get(0).findElements(By.xpath("child::*"));
	}

	private List<WebElement> findRows(Locator additional, WebElement table) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				//mark headers row, if it needed
				List<WebElement> rows = table.findElement(By.xpath("child::" + tag_tbody)).findElements(By.xpath("child::" + tag_tr));
				List<WebElement> cells = rows.get(0).findElements(By.xpath("child::" + tag_th));
				boolean empty = cells.isEmpty();

				if (additional != null)
				{
					MatcherSelenium by = new MatcherSelenium(this.info, ControlKind.Row, additional);
					List<WebElement> elements = table.findElement(By.xpath("child::" + tag_tbody)).findElements(by);
					unmarkRowIsHeader(table);
					return elements;
				}
				else
				{
					if (empty)
					{
						unmarkRowIsHeader(table);
					}
					return table.findElement(By.xpath("child::" + tag_tbody)).findElements(this.selectRowsWithoutHeader());
				}
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	private Map<String, ValueAndColor> valueFromRow(WebElement row, List<String> headers) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				Map<String, ValueAndColor> res = new LinkedHashMap<String, ValueAndColor>();

				List<WebElement> cells = row.findElements(By.xpath("child::" + tag_td));
				for (int i = 0; i < cells.size(); i++)
				{
					WebElement cell = cells.get(i);
					String name = i < headers.size() ? headers.get(i) : String.valueOf(i);
					String value = cell.getText();
					String colorFG = cell.getCssValue("color");
					String colorBG = cell.getCssValue("background-color");
					res.put(name, new ValueAndColor(value, getColor(colorFG), getColor(colorBG)));
				}

				return res;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}

	private boolean rowMatches(WebElement row, ICondition valueCondition, ICondition colorCondition, List<String> headers) throws Exception
	{
		Exception real = null;
		int repeat = 1;
		do
		{
			try
			{
				List<WebElement> cells = row.findElements(By.xpath("child::" + tag_td));
				Map<String, Object> map = new LinkedHashMap<>();
				for (int i = 0; i < cells.size(); i++)
				{
					WebElement cell = cells.get(i);
					map.put(i < headers.size() ? headers.get(i) : null, cell.getText());
				}
				if (valueCondition != null)
				{
					if (!valueCondition.isMatched(map))
					{
						return false;
					}
				}
				if (colorCondition != null)
				{
					if (!colorCondition.isMatched(map))
					{
						return false;
					}
				}
				return true;
			}
			catch (StaleElementReferenceException e)
			{
				real = e;
				logger.debug("Element is no longer attached to the DOM. Try in SeleniumOperationExecutor : " + repeat);
			}
		}
		while (++repeat < repeatLimit);
		throw real;
	}
	/* //reserve code in case of flicker and double clicking in the IE
	private void clickByJavascript(WebElement element)
	{
		driver.executeScript("arguments[0].click();", element);
	}
	
	private void clickByJavascriptByXY(WebElement element, int _x, int _y)
	{
		int x = element.getLocation().x + _x;
		int y = element.getLocation().y + _y;
		driver.executeScript("document.elementFromPoint(" + x + "," + y + ").click()");
	}*/
	
	private void scrollToElement(WebElement element)
	{
		if(!(element instanceof DummyWebElement))
		{
			driver.executeScript(SCROLL_TO_SCRIPT,element);
		}
	}

	private By selectRowsWithoutHeader()
	{
		return By.xpath(String.format("child::%s[not(@%s)]", tag_tr, markAttribute));
	}

	private By selectRowLikeHeader()
	{
		return By.xpath(String.format("child::%s[@%s]", tag_tr, markAttribute));
	}

	private void markRowIsHeader(WebElement row, boolean isSet)
	{
		if (isSet)
		{
			this.driver.executeScript("arguments[0].setAttribute(\"" + markAttribute + "\", \"true\")", row);
		}
		else
		{
			this.driver.executeScript("arguments[0].removeAttribute(\"" + markAttribute + "\")", row);
		}
	}

	private void unmarkRowIsHeader(WebElement grid)
	{
		List<WebElement> elements = grid.findElement(By.xpath("child::"+tag_tbody)).findElements(selectRowLikeHeader());
		if (!elements.isEmpty())
		{
			markRowIsHeader(elements.get(0), false);
		}
	}

	private static String loadScript(String path)
	{
		Scanner scanner = new Scanner(SeleniumOperationExecutor.class.getResourceAsStream(path));
		StringBuilder ret = new StringBuilder();
		while (scanner.hasNextLine())
		{
			ret.append(scanner.nextLine()).append("\n");
		}
		return ret.toString();
	}

	private static Elements findRows(Document doc) throws Exception
	{
		Element first = doc.select(tag_tbody).first();
		if (first == null)
		{
			throw new Exception("Can't find tag tbody in current table");
		}
		return first.children();
	}
	//endregion

	private static final String MOVE_TO_SCRIPT =
			"var myMoveToFunction = function(elem) {\n"+
			"   if (document.createEvent) {\n"+
			"       var evObj = document.createEvent('MouseEvents');\n"+
			"       evObj.initEvent('mouseover',true, false);\n"+
			"       elem.dispatchEvent(evObj);\n"+
			"   } else if (document.createEventObject) {\n"+
			"       elem.fireEvent('onmouseover');\n"+
			"   };\n"+
			"};\n"+
			"myMoveToFunction(arguments[0])";

	//TODO need normal scroll to function
	private static final String SCROLL_TO_SCRIPT = loadScript("js/scrollTo.js");

	private String markAttribute = "seleniummarkattribute";

	private boolean isShiftDown = false;
	private boolean isCtrlDown = false;
	private boolean isAltDown = false;

	private Actions customAction;
	private WebDriverListenerNew driver;
	private PluginInfo info;
	private Logger logger;
}
