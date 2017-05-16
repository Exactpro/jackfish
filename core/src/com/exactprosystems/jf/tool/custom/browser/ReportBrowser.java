////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2016, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.tool.custom.browser;

import com.exactprosystems.jf.api.common.Sys;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class ReportBrowser extends BorderPane
{
	private TabPane tabPane;
	private File reportFile;

	public ReportBrowser(File reportFile)
	{
		this.getStyleClass().add(CssVariables.BROWSER);
		this.reportFile = reportFile;
		initTabPane();
		initToolbar();
	}

	public String getMatrix()
	{
		try
		{
			String copyName = "Copy";
			CustomBrowserTab selectedItem = (CustomBrowserTab) this.tabPane.getSelectionModel().getSelectedItem();
			Document document = selectedItem.engine.getDocument();
			String content = document.getElementsByTagName("pre").item(0).getTextContent();
			if (content.startsWith(copyName))
			{
				content = content.substring(copyName.length());
			}

			return content;
		}
		catch (Exception e)
		{
			return "";
		}
	}

	private void initToolbar()
	{
		ToolBar toolBar = new ToolBar();

		Button reload = new Button();
		Common.customizeLabeled(reload, CssVariables.TRANSPARENT_BACKGROUND, CssVariables.Icons.RELOAD);
		reload.setOnAction(e -> ((CustomBrowserTab) this.tabPane.getSelectionModel().getSelectedItem()).reload());
		toolBar.getItems().add(reload);

		Button back = new Button();
		Common.customizeLabeled(back, CssVariables.TRANSPARENT_BACKGROUND, CssVariables.Icons.GO_BACK);
		back.setOnAction(e -> ((CustomBrowserTab) this.tabPane.getSelectionModel().getSelectedItem()).back());
		toolBar.getItems().add(back);

		Button forward = new Button();
		Common.customizeLabeled(forward, CssVariables.TRANSPARENT_BACKGROUND, CssVariables.Icons.GO_FORWARD);
		forward.setOnAction(e -> ((CustomBrowserTab) this.tabPane.getSelectionModel().getSelectedItem()).forward());
		toolBar.getItems().add(forward);

		this.setTop(toolBar);
	}

	private void initTabPane()
	{
		this.tabPane = new TabPane();
		this.setCenter(this.tabPane);
		CustomBrowserTab mainTab = new CustomBrowserTab();
		mainTab.load(this.reportFile);
		mainTab.setClosable(false);
		this.tabPane.getTabs().add(mainTab);
	}

	private class CustomBrowserTab extends Tab
	{
		private WebEngine engine;
		private Hyperlink crossButton;
		private Text textTab;
		private Boolean ctrlC = false;

		private void createContextMenu(WebView webView)
		{
			ContextMenu contextMenu = new ContextMenu();
			MenuItem copy = new MenuItem("Copy");
			copy.setOnAction(e ->
			{
				String selection;
				selection = (String) webView.getEngine().executeScript("window.getSelection().toString()");
				StringSelection stringSelection = new StringSelection(selection);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
			});

			contextMenu.getItems().addAll(copy);

			webView.setOnMousePressed(e ->
			{
				if (e.getButton() == MouseButton.SECONDARY)
				{
					contextMenu.show(webView, e.getScreenX(), e.getScreenY());
				}
				else
				{
					contextMenu.hide();
				}
			});
		}

		private void createCtrlCHandler(WebView view) {
			view.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
				if(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event))
				{
					ctrlC = true;
				}
			});
			view.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
				if(event.getCode().equals(KeyCode.CONTROL) && ctrlC)
				{
					ctrlC = false;
					Sys.copyToClipboard((String) view.getEngine().executeScript("window.getSelection().toString()"));
				}
			});
		}

		public CustomBrowserTab()
		{
			WebView view = new WebView();
			this.engine = view.getEngine();
			this.setContent(view);
			view.setContextMenuEnabled(false);
			createContextMenu(view);
			createCtrlCHandler(view);

			textTab = new Text();
			this.textTab.setText("New tab...");
			Worker<Void> loadWorker = this.engine.getLoadWorker();
			loadWorker.stateProperty().addListener((observable, oldValue, newValue) ->
			{
				if (newValue == Worker.State.SUCCEEDED)
				{
					String title = this.engine.getTitle();
					if (title == null)
					{
						String location = this.engine.getLocation();
						title = location.substring(location.lastIndexOf(File.separator) + 1);
					}
					this.textTab.setText(title);
				}
			});

			this.setClosable(false);
			this.crossButton = new Hyperlink();
			Image image = new Image(CssVariables.Icons.CLOSE_BUTTON_ICON);
			this.crossButton.setGraphic(new ImageView(image));
			this.crossButton.setFocusTraversable(false);

			HBox box = new HBox();
			box.setAlignment(Pos.CENTER_RIGHT);
			box.getChildren().addAll(this.textTab, this.crossButton);
			this.setGraphic(box);

			this.engine.setCreatePopupHandler(param ->
			{
				CustomBrowserTab customTab = new CustomBrowserTab();
				this.getTabPane().getTabs().add(customTab);
				this.getTabPane().getSelectionModel().select(customTab);
				// RM38890 the tab needs be resized for display image
				changeSize(customTab);
				return customTab.engine;
			});

			this.crossButton.setDisable(true);
			this.crossButton.setVisible(false);
			this.setOnSelectionChanged(arg0 ->
			{
				crossButton.setDisable(!isSelected());
				crossButton.setVisible(isSelected());
			});

			this.crossButton.setOnAction(actionEvent ->
			{
				ObservableList<Tab> tabs = this.getTabPane().getTabs();
				tabs.remove(this);
				if (tabs.size() == 0)
				{
					ReportBrowser.this.getScene().getWindow().hide();
				}
			});
		}

		private void changeSize(CustomBrowserTab customTab)
		{
			Timer animTimer = new Timer();
			Stage stage = (Stage) customTab.getTabPane().getScene().getWindow();
			animTimer.scheduleAtFixedRate(new TimerTask()
			{
				int i = 0;

				@Override
				public void run()
				{
					if (i < 1)
					{
						stage.setWidth(stage.getWidth() + 1);
						stage.setHeight(stage.getHeight() + 1);
					}
					else
					{
						this.cancel();
					}
					i++;
				}

			}, 0, 25);
		}

		public void load(File file)
		{
			this.engine.load(file.toURI().toASCIIString());
		}

		public void reload()
		{
			this.engine.reload();
		}

		public void back()
		{
			final WebHistory history = this.engine.getHistory();
			ObservableList<WebHistory.Entry> entryList = history.getEntries();
			int currentIndex = history.getCurrentIndex();
			if (currentIndex > 0)
			{
				Platform.runLater(() -> history.go(-1));
			}
			String url = entryList.get(currentIndex > 0 ? currentIndex - 1 : currentIndex).getUrl();
			this.engine.load(url);
		}

		public void forward()
		{
			final WebHistory history = this.engine.getHistory();
			ObservableList<WebHistory.Entry> entryList = history.getEntries();
			int currentIndex = history.getCurrentIndex();
			if (currentIndex + 1 < this.engine.getHistory().getEntries().size())
			{
				Platform.runLater(() -> history.go(1));
			}
			String url = entryList.get(currentIndex < entryList.size() - 1 ? currentIndex + 1 : currentIndex).getUrl();
			this.engine.load(url);
		}
	}
}