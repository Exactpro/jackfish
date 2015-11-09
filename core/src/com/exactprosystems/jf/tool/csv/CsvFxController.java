////////////////////////////////////////////////////////////////////////////////
//Copyright (c) 2009-2015, Exactpro Systems, LLC
//Quality Assurance & Related Development for Innovative Trading Systems.
//All rights reserved.
//This is unpublished, licensed software, confidential and proprietary
//information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.tool.csv;

import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.functions.Table;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.custom.grideditor.DataProvider;
import com.exactprosystems.jf.tool.custom.grideditor.SpreadsheetView;
import com.exactprosystems.jf.tool.custom.grideditor.TableDataProvider;
import com.exactprosystems.jf.tool.custom.tab.CustomTab;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class CsvFxController implements Initializable, ContainingParent
{
	public GridPane					grid;
	public SpreadsheetView 			view;

	private Parent					pane;
	private CsvFx					model;
	private CustomTab				tab;
	private DataProvider<String>	provider;

	// ----------------------------------------------------------------------------------------------
	// Event handlers
	// ----------------------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------------------
	// Interface Initializable
	// ----------------------------------------------------------------------------------------------
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
	}

	// ----------------------------------------------------------------------------------------------
	// Interface ContainingParent
	// ----------------------------------------------------------------------------------------------
	@Override
	public void setParent(Parent parent)
	{
		this.pane = parent;
	}

	// ----------------------------------------------------------------------------------------------
	// Public methods
	// ----------------------------------------------------------------------------------------------
	public void init(CsvFx model, Settings settings)
	{
		this.model = model;
		
		this.tab = Common.createTab(model);
		this.tab.setContent(this.pane);

		Platform.runLater(() ->
		{
			Common.getTabPane().getTabs().add(this.tab);
			Common.getTabPane().getSelectionModel().select(this.tab);
		});
	}

	public void saved(String name)
	{
		this.tab.saved(name);
	}

	public void close() throws Exception
	{
		this.tab.close();
		Common.getTabPane().getTabs().remove(this.tab);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// display* methods
	// ------------------------------------------------------------------------------------------------------------------
	public void displayTitle(String title)
	{
		Platform.runLater(() -> this.tab.setTitle(title));
	}

	public void displayTable(Table table)
	{
		Platform.runLater(() -> 
		{
			this.provider = new TableDataProvider(table);
			this.view = new SpreadsheetView(this.provider);
			this.grid.add(this.view, 0, 0);
		});
	}

	// ------------------------------------------------------------------------------------------------------------------

}
