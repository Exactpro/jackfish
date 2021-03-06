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
import com.exactprosystems.jf.api.common.Converter;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.error.app.ElementNotFoundException;
import com.exactprosystems.jf.api.error.app.NullParameterException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MatcherFx
{
	private static Logger logger = null;
	private Locator locator;
	static final String itemName = "item";
	private PluginInfo info;
	private NodeList   nodelist;

	private EventTarget owner;

	private static Set<Class<?>> parents;

	public MatcherFx(PluginInfo info, Locator locator, EventTarget owner) throws RemoteException
	{
		if (locator == null)
		{
			throw new NullParameterException("locator");
		}
		this.locator = locator;
		this.info = info;
		this.owner = owner == null ? UtilsFx.currentRoot() : owner;

		String locatorXpath = this.locator.getXpath();
		if (!Str.IsNullOrEmpty(locatorXpath) && owner != null)
		{
			try
			{
				org.w3c.dom.Document document = createDocument(this.info, owner, true, false);

				XPathFactory factory = XPathFactory.newInstance();
				XPath xPath = factory.newXPath();
				org.w3c.dom.Node root = document;

				try
				{
					XPathExpression compile = xPath.compile("/*");
					root = (org.w3c.dom.Node) compile.evaluate(document, XPathConstants.NODE);
				}
				catch (XPathExpressionException e)
				{
				}

				this.nodelist = (NodeList) xPath.compile(locatorXpath).evaluate(root, XPathConstants.NODESET);
				logger.debug("Found by xpath : " + nodelist.getLength());
			}
			catch (Exception pe)
			{
				logger.error(pe.getMessage(), pe);
				throw new ElementNotFoundException("Wrong xpath: " + locatorXpath, locator);
			}
		}

		logger.debug("=========================================");
		logger.debug("Matcher locator = " + locator);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("MatcherFx :").append("\n");
		if (nodelist != null)
		{
			builder.append("found by xpath : ").append(nodelist.getLength()).append("\n");
		}
		if (locator != null)
		{
			builder.append("locator : ").append(locator);
		}
		return builder.toString();
	}

	//region work with find Nodes
	public List<EventTarget> findAll()
	{
		List<EventTarget> nodes = new ArrayList<>();
		collect(this.owner, nodes);
		return nodes.stream()
				.filter(this::isMatches)
				.collect(Collectors.toList());
	}

	public List<EventTarget> findAllDescedants()
	{
		List<EventTarget> nodes = new ArrayList<>();
		collect(this.owner, nodes);
		return nodes;
	}

	private boolean isMatches(EventTarget target)
	{
		if (target == null)
		{
			return false;
		}
		if (target instanceof Stage)
		{
			Platform.runLater(((Stage) target)::toFront);
		}
		boolean result = isVisible(target);
		if (this.locator.getVisibility() == Visibility.Visible)
		{
			result = true;
		}
		if (!result)
		{
			return false;
		}

		if (this.nodelist != null)
		{
			result = IntStream.range(0, nodelist.getLength())
					.mapToObj(nodelist::item)
					.anyMatch(n -> n.getUserData(itemName).equals(target));
			if (!Str.IsNullOrEmpty(this.locator.getXpath()))
			{
				return result;
			}
		}

		//we need check that target is instance of locator.getControlKind()
		boolean classIsFound = checkClass(target);

		if (!classIsFound)
		{
			return false;
		}

		result = part(result, this.locator, LocatorFieldKind.UID,     getId(target));
		result = part(result, this.locator, LocatorFieldKind.CLAZZ,   getClass(target));
		result = part(result, this.locator, LocatorFieldKind.TEXT,    getText(target));
		result = part(result, this.locator, LocatorFieldKind.TOOLTIP, getToolTip(target));
		result = part(result, this.locator, LocatorFieldKind.TITLE,   getTitle(target));

		return result;
	}

	private boolean checkClass(EventTarget target)
	{
		Set<String> strClasses = this.info.nodeByControlKind(locator.getControlKind());
		for (String strClass : strClasses)
		{
			if (strClass.equals(PluginInfo.ANY_TYPE))
			{
				return true;
			}
			try
			{
				Class<?> clazz = Class.forName(strClass);
				if (clazz.isAssignableFrom(target.getClass()))
				{
					return true;
				}
			}
			catch (ClassNotFoundException ignored)
			{}
		}
		return false;
	}

	private void collect(EventTarget eventTarget, List<EventTarget> nodes)
	{
		nodes.add(eventTarget);
		if (eventTarget instanceof RootContainer)
		{
			//this means that we pass owner as null for searching of locator
			RootContainer rootContainer = (RootContainer) eventTarget;
			rootContainer.getWindows().forEach(window -> collect(window, nodes));
		}
		else if (eventTarget instanceof Window)
		{
			collect(((Window) eventTarget).getScene().getRoot(), nodes);
		}
		else if (eventTarget instanceof Parent)
		{
			ObservableList<Node> children = ((Parent) eventTarget).getChildrenUnmodifiable();
			children.forEach(child -> collect(child, nodes));
		}
	}

	private boolean part(boolean result, Locator locator, LocatorFieldKind kind, String objText)
	{
		if (kind == null)
		{
			return result;
		}
		boolean weak = locator.isWeak();
		String locatorText = Str.asString(locator.get(kind));
		if (Str.IsNullOrEmpty(locatorText))
		{
			return result;
		}

		if (kind == LocatorFieldKind.CLAZZ && Objects.nonNull(objText))
		{
			String[] classes = locatorText.split(" ");
			for (String aClass : classes)
			{
				if (aClass.startsWith("!"))
				{
					result = result && !objText.contains(aClass.substring(1));
				}
				else
				{
					result = result && objText.contains(aClass);
				}
			}
			return result;
		}

		if (objText != null)
		{
			if (weak)
			{
				result = result && objText.contains(locatorText);
			}
			else
			{
				result = result && objText.equals(locatorText);
			}
		}
		else
		{
			return false;
		}

		return result;
	}
	//endregion

	//region work with xml document
	public static org.w3c.dom.Document createDocument(PluginInfo info, EventTarget owner, boolean addItems, boolean addRectangles) throws ParserConfigurationException
	{
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		org.w3c.dom.Document document = builder.newDocument();
		buildDom(info, document, document, owner, addItems, addRectangles);

		return document;
	}

	private static void buildDom(PluginInfo info, org.w3c.dom.Document document, org.w3c.dom.Node current, EventTarget component, boolean addItems, boolean addRectangles)
	{
		if (component == null)
		{
			return;
		}

		Element node;
		String simpleName = component.getClass().getSimpleName();
		if (simpleName.isEmpty())
		{
			simpleName = component.getClass().getName();
		}
		String tagName = simpleName.replaceAll("\\$", "_");
		try
		{
			node = document.createElement(tagName);
		}
		catch (DOMException e)
		{
			logger.error("Current component : " + component);
			logger.error("Error on create element with tag : '" + tagName + "'. Component class simple name : '" + simpleName + "'.");
			throw e;
		}
		if (addItems)
		{
			node.setUserData(itemName, component, null);
		}
		if (addRectangles)
		{
			node.setAttribute(IRemoteApplication.rectangleName, Converter.rectangleToString(getRect(component, true)));
			node.setAttribute(IRemoteApplication.visibleName, "" + isVisible(component));
		}

		String className = info.attributeName(LocatorFieldKind.CLAZZ);
		String idName = info.attributeName(LocatorFieldKind.UID);
		String titleName = info.attributeName(LocatorFieldKind.TITLE);
		String tooltipName = info.attributeName(LocatorFieldKind.TOOLTIP);


		setNodeAttribute(node, titleName, getTitle(component));
		setNodeAttribute(node, tooltipName, getToolTip(component));
		setNodeAttribute(node, idName, getId(component));
		setNodeAttribute(node, className, getClass(component));
		addBaseClass(node, component, info);
		String textContent = getText(component);
		if (!Str.IsNullOrEmpty(textContent))
		{
			node.setTextContent(textContent);
		}

		current.appendChild(node);
		if (component instanceof Dialog<?>)
		{
			buildDom(info, document, node, ((Dialog<?>) component).getDialogPane().getScene().getWindow(), addItems, addRectangles);
		}
		if (component instanceof Window)
		{
			buildDom(info, document, node, ((Window) component).getScene().getRoot(), addItems, addRectangles);
		}
		else if (component instanceof RootContainer)
		{
			RootContainer rootContainer = (RootContainer) component;
			logger.debug("Found root container : " + rootContainer.toString());
			rootContainer.getWindows().forEach(window -> buildDom(info, document, node, window, addItems, addRectangles));
		}
		else if (component instanceof Parent)
		{
			Parent container = (Parent) component;

			for (Node child : container.getChildrenUnmodifiable())
			{
				buildDom(info, document, node, child, addItems, addRectangles);
			}
		}
	}

	private static void setNodeAttribute(Element node, String attrName, String attrValue)
	{
		if (!Str.IsNullOrEmpty(attrValue))
		{
			node.setAttribute(attrName, attrValue);
		}
	}

	private static void addBaseClass(Element node, EventTarget target, PluginInfo info)
	{
		Set<Class<?>> allParents = getAllParents(info);
		Class<?> clazz = target.getClass();
		while (clazz != null)
		{
			if (allParents.contains(clazz))
			{
				node.setAttribute(IRemoteApplication.baseParnetName, clazz.getName());
				break;
			}
			clazz = clazz.getSuperclass();
		}
	}

	private static Set<Class<?>> getAllParents(PluginInfo info)
	{
		if (parents == null)
		{
			parents = Arrays.stream(ControlKind.values())
					.map(info::nodeByControlKind)
					.filter(Objects::nonNull)
					.flatMap(Set::stream)
					.map(str ->
					{
						try
						{
							return Class.forName(str);
						}
						catch (ClassNotFoundException ignored) {;}
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}
		return parents;
	}

	//endregion

	//region public static methods
	public static String getText(EventTarget target)
	{
		if (target instanceof TableView)
		{
			StringBuilder sb = new StringBuilder();
			TableView table = (TableView) target;
			for (int rowNum = 0; rowNum < table.getItems().size(); ++rowNum)
			{
				for (int colNum = 0; colNum < table.getColumns().size(); ++colNum)
				{
					TableColumn column = ((TableColumn) ((TableView) target).getColumns().get(colNum));
					String someText = String.valueOf(column.getCellObservableValue(rowNum).getValue());
					if (someText != null && !someText.isEmpty())
					{
						sb.append(someText);
						sb.append('|');
					}
				}
				sb.append('\n');
			}
			return sb.toString();
		}
		else if (target instanceof Text)
		{
			return ((Text) target).getText();
		}
		else if (target instanceof Tooltip)
		{
			Tooltip tooltip = (Tooltip) target;
			StringBuilder sb = new StringBuilder();
			if (!Str.IsNullOrEmpty(tooltip.getText()))
			{
				sb.append(tooltip.getText());
			}
			Node graphic = tooltip.getGraphic();
			if (graphic != null)
			{
				String graphicText = getText(graphic);
				if (!Str.IsNullOrEmpty(graphicText))
				{
					sb.append(graphicText);
				}
			}
			return sb.toString();
		}
		else if (target instanceof TextInputControl)
		{
			return ((TextInputControl) target).getText();
		}
		else if (target instanceof Pane)
		{
			StringBuilder sb = new StringBuilder();
			collectAllText(((Pane) target), sb);
			return sb.toString();
		}
		else if (target instanceof Stage)
		{
			return ((Stage) target).getTitle();
		}
		else if (target instanceof Dialog)
		{
			return ((Dialog) target).getTitle();
		}
		else if (target instanceof Labeled)
		{
			Labeled labeled = (Labeled) target;
			StringBuilder sb = new StringBuilder();
			String text = labeled.getText();
			if (!Str.IsNullOrEmpty(text))
			{
				sb.append(text);
			}
			Node graphic = labeled.getGraphic();
			if (graphic != null)
			{
				String graphicText = getText(graphic);
				if (!Str.IsNullOrEmpty(graphicText))
				{
					sb.append(graphicText);
				}
			}
			return sb.toString();
		}
		else if (target instanceof ComboBox)
		{
			ComboBox comboBox = (ComboBox) target;
			return comboBox.isEditable() ? comboBox.getEditor().getText() : Str.asString(comboBox.getValue());
		}
		else if (target instanceof TreeItem)
		{
			TreeItem treeItem = (TreeItem) target;
			Object value = treeItem.getValue();
			if(value != null)
			{
				return treeItem.getValue().toString();
			}
			return "";
		}
		else
		{
			return null;
		}
	}

	private static void collectAllText(Pane pane, StringBuilder sb)
	{
		ObservableList<Node> children = pane.getChildren();
		children.forEach(node -> sb.append(getText(node)));
	}

	public static String getAction(EventTarget target)
	{
		String objAction = null;

		if (target instanceof ComboBox)
		{
			objAction = ((ComboBox<?>) target).getOnAction().toString();
		}
		else if (target instanceof Button)
		{
			objAction = ((Button) target).getOnAction().toString();
		}
		else if (target instanceof RadioButton)
		{
			objAction = ((RadioButton) target).getOnAction().toString();
		}
		else if (target instanceof ToggleButton)
		{
			objAction = ((ToggleButton) target).getOnAction().toString();
		}
		else if (target instanceof MenuItem)
		{
			objAction = ((MenuItem) target).getOnAction().toString();
		}

		return objAction;
	}

	public static String getTitle(EventTarget obj)
	{
		String objTitle = null;

		if (obj instanceof Stage)
		{
			objTitle = ((Stage) obj).getTitle();
		}
		else if (obj instanceof Dialog<?>)
		{
			objTitle = ((Dialog) obj).getTitle();
		}
		return objTitle;
	}

	public static String getToolTip(EventTarget obj)
	{
		String objText = null;

		if (obj instanceof Control)
		{
			Tooltip tooltip = ((Control) obj).getTooltip();
			if (tooltip != null)
			{
				objText = tooltip.getText();
			}
		}

		return objText;
	}

	public static String getId(EventTarget target)
	{
		String objText = null;

		if (target instanceof Node)
		{
			objText = ((Node) target).getId();
		}

		return objText;
	}

	public static String getClass(EventTarget target)
	{
		String objText = null;

		if (target instanceof Node)
		{
			objText = ((Node) target).getStyleClass().stream().collect(Collectors.joining(" "));
		}

		return objText;
	}

	public static boolean isVisible(EventTarget target)
	{
		if (target instanceof TreeCell)
		{
			return ((TreeCell) target).isVisible();
		}
		else if (target instanceof Scene)
		{
			return ((Scene) target).getWindow().isShowing();
		}
		else if (target instanceof Dialog)
		{
			return ((Dialog) target).isShowing();
		}
		else if (target instanceof MenuItem)
		{
			return ((MenuItem) target).isVisible();
		}
		else if (target instanceof Node)
		{
			return ((Node) target).isVisible();
		}
		else if (target instanceof Window)
		{
			return ((Window) target).isShowing();
		}
		return false;
	}

	public static Rectangle getRect(EventTarget target, boolean getScene)
	{
		if (target instanceof Dialog<?>)
		{
			Dialog<?> dialog = (Dialog<?>) target;
			return new Rectangle((int)dialog.getX(), (int)dialog.getY(), (int)dialog.getWidth(), (int)dialog.getHeight());
		}
		if (target instanceof Window)
		{
			if(getScene)
			{
				Scene scene = ((Window) target).getScene();
				Node node = scene.getRoot();
				return getRectangleFromNode(node, getScene);
			}
			else
			{
				Window window = (Window) target;
				return new Rectangle((int) window.getX(), (int) window.getY(), (int) window.getWidth(), (int) window.getHeight());
			}
		}
		if (!(target instanceof Node) || target instanceof RootContainer)
		{
			return new Rectangle(0, 0, 0, 0);
		}
		Node node = (Node) target;
		return getRectangleFromNode(node, getScene);
	}

	public static String targetToString(EventTarget target)
	{
		if (target instanceof Stage)
		{
			return "Stage : " + ((Stage) target).getTitle();
		}
		else if (target instanceof Dialog)
		{
			return "Dialog : " + ((Dialog) target).getTitle();
		}
		else if (target instanceof Scene)
		{
			return "SceneRoot : " + ((Scene) target).getRoot();
		}
		else if (target instanceof Window)
		{
			return "Window : " + target.getClass();
		}
		return target.toString();
	}
	//endregion

	static void setLogger(Logger logger)
	{
		MatcherFx.logger = logger;
	}

	private static Rectangle getRectangleFromNode(Node node, boolean getScene)
	{
		if (node.isVisible())
		{
			Bounds screenBounds;
			if(getScene)
			{
				screenBounds = node.localToScene(node.getBoundsInLocal());
			}
			else
			{
				screenBounds = node.localToScreen(node.getBoundsInLocal());
			}

			int x = (int) screenBounds.getMinX();
			int y = (int) screenBounds.getMinY();
			int width = (int) screenBounds.getWidth();
			int height = (int) screenBounds.getHeight();
			return new Rectangle(x, y, width, height);
		}
		return new Rectangle(0, 0, 0, 0);
	}
}