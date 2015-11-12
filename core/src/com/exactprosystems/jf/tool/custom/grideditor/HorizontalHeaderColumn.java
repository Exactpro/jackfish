////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.tool.custom.grideditor;

import com.sun.javafx.scene.control.skin.NestedTableColumnHeader;
import com.sun.javafx.scene.control.skin.TableColumnHeader;
import com.sun.javafx.scene.control.skin.TableViewSkinBase;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumnBase;

public class HorizontalHeaderColumn extends NestedTableColumnHeader
{
	int lastColumnResized = -1;

	public HorizontalHeaderColumn(TableViewSkinBase<?, ?, ?, ?, ?, ?> skin, TableColumnBase<?, ?> tc)
	{
		super(skin, tc);
		widthProperty().addListener((Observable observable) -> {
			((GridViewSkin) skin).hBarValue.clear();
			((GridViewSkin) skin).rectangleSelection.updateRectangle();
		});

		columnReorderLine.layoutXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			HorizontalHeader headerRow = (HorizontalHeader) skin.getTableHeaderRow();
			GridViewSkin mySkin = ((GridViewSkin) skin);
			if (newValue.intValue() == 0 && lastColumnResized >= 0)
			{
				if (headerRow.selectedColumns.get(lastColumnResized))
				{
					double width1 = mySkin.getColumns().get(lastColumnResized).getWidth();
					for (int i = headerRow.selectedColumns.nextSetBit(0); i >= 0; i = headerRow.selectedColumns.nextSetBit(i + 1))
					{
						mySkin.getColumns().get(i).setPrefWidth(width1);
					}
				}
			}
		});
	}

	@Override
	protected TableColumnHeader createTableColumnHeader(final TableColumnBase col)
	{
		TableViewSkinBase<?, ?, ?, ?, ?, TableColumnBase<?, ?>> tableViewSkin = getTableViewSkin();
		if (col.getColumns().isEmpty())
		{
			final TableColumnHeader columnHeader = new TableColumnHeader(tableViewSkin, col);
			columnHeader.setOnMousePressed(mouseEvent -> {
				if (mouseEvent.getClickCount() == 2 && mouseEvent.isPrimaryButtonDown())
				{
					((GridViewSkin) (Object) tableViewSkin).resize(col, -1);
				}
			});
			return columnHeader;
		}
		else
		{
			return new HorizontalHeaderColumn(getTableViewSkin(), col);
		}
	}

	@Override
	protected void layoutChildren()
	{
		super.layoutChildren();
		layoutFixedColumns();
	}

	public void layoutFixedColumns()
	{
		SpreadsheetHandle handle = ((GridViewSkin) (Object) getTableViewSkin()).handle;
		final SpreadsheetView spreadsheetView = handle.getView();
		if (handle.getCellsViewSkin() == null || getChildren().isEmpty())
		{
			return;
		}
		int max = getColumnHeaders().size();
		max = max > spreadsheetView.getColumns().size() ? spreadsheetView.getColumns().size() : max;
		for (int j = 0; j < max; j++)
		{
			final TableColumnHeader n = getColumnHeaders().get(j);
			n.setPrefHeight(24.0);
		}

	}
}
