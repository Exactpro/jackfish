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

package com.exactprosystems.jf.tool.custom.treetable;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.documents.matrix.parser.items.End;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItemState;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.custom.skin.CustomTreeTableViewSkin;
import com.exactprosystems.jf.tool.matrix.MatrixFx;
import com.exactprosystems.jf.tool.matrix.params.ParametersPane;
import com.exactprosystems.jf.tool.settings.SettingsPanel;
import com.sun.javafx.scene.control.skin.TreeTableViewSkin;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MatrixTreeView extends TreeTableView<MatrixItem>
{
	private MatrixFx matrix;

	private boolean isTracing;
	private boolean needExpand;
	private long lastTime = 0;

	public MatrixTreeView()
	{
		super(null);
		this.setSkin(new CustomTreeTableViewSkin<>(this));
		this.setShowRoot(false);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		this.getStyleClass().addAll(CssVariables.CUSTOM_TREE_TABLE_VIEW);
		initTable();
		TreeTableColumn[] columns = this.getColumns().toArray(new TreeTableColumn[this.getColumns().size()]);
		this.getColumns().addListener(new ListChangeListener<TreeTableColumn<MatrixItem, ?>>()
		{
			public boolean suspended;

			@Override
			public void onChanged(Change<? extends TreeTableColumn<MatrixItem, ?>> change)
			{
				change.next();
				if (change.wasReplaced() && !suspended) {
					this.suspended = true;
					getColumns().setAll(columns);
					this.suspended = false;
				}
			}
		});
		this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				int row = this.getRow(newValue);
				scrollTo(row);
			}
		});
	}

	public void setNeedExpand(boolean flag)
	{
		this.needExpand = flag;
	}

	public void init(MatrixFx matrix, Settings settings, ContextMenu contextMenu)
	{
		this.matrix = matrix;

		setRoot(new TreeItem<>(matrix.getRoot()));

		setRowFactory(treeView -> {
			MatrixTreeRow row = new MatrixTreeRow(contextMenu);
			shortCuts(row, settings);
			return row;
		});
	}

	public void setCurrent(TreeItem<MatrixItem> treeItem, boolean needExpand)
	{
		if (treeItem != null)
		{

			TreeItem<MatrixItem> parent = treeItem.getParent();
			while (parent != null)
			{
				parent.setExpanded(true);
				parent = parent.getParent();
			}
			if (needExpand)
			{
				expand(treeItem, !this.needExpand);
			}
			final int row = getRow(treeItem);
			Task<Void> task = new Task<Void>()
			{
				@Override
				protected Void call() throws Exception
				{
					Thread.sleep(30);
					return null;
				}
			};
			task.setOnSucceeded(event -> {
				getSelectionModel().clearAndSelect(row);
				scrollTo(row);
			});
			new Thread(task).start();
		}
	}

	public void refresh()
	{
		long current = System.currentTimeMillis();
		if (Math.abs(current - lastTime) > 199)
		{
			Optional.ofNullable(this.getColumns().get(0)).ifPresent(col -> Common.runLater(() ->{
				col.setVisible(false);
				col.setVisible(true);
			}));
			lastTime = current;
		}
	}

	public void forceRefresh()
	{
		Optional.ofNullable(this.getColumns().get(0)).ifPresent(col -> Common.runLater(() ->{
			col.setVisible(false);
			col.setVisible(true);
		}));
	}

	public void refreshParameters(MatrixItem item, int selectedIndex)
	{
		TreeItem<MatrixItem> treeItem = find(item);
		if (treeItem != null)
		{
			GridPane layout = (GridPane) treeItem.getValue().getLayout();
			{
				layout.getChildren().stream().filter(pane -> pane instanceof ParametersPane).forEach(pane -> Common.runLater(() -> {
					((ParametersPane) pane).refreshParameters(selectedIndex);
				}));
			}
		}
	}

	public void expandAll()
	{
		expand(getRoot(), true);
	}

	public void collapseAll()
	{
		expand(getRoot(), false);
	}

	public void expand(TreeItem<MatrixItem> rootItem, boolean flag)
	{
		if (rootItem == null)
		{
			return;
		}
		rootItem.setExpanded(flag);
		rootItem.getChildren().forEach(item ->
		{
			item.setExpanded(flag);
			expand(item, flag);
		});
	}

	public List<MatrixItem> currentItems()
	{
		return getSelectionModel().getSelectedCells().stream().map(TreeTablePosition::getTreeItem).map(TreeItem::getValue).collect(Collectors.toList());
	}

	public MatrixItem currentItem()
	{
		TreeItem<MatrixItem> selectedItem = getSelectionModel().getSelectedItem();
		return selectedItem != null ? selectedItem.getValue() : null;
	}

	public void setTracing(boolean flag)
	{
		this.isTracing = flag;
	}

	public boolean isTracing()
	{
		return isTracing;
	}

	public TreeItem<MatrixItem> find(MatrixItem item)
	{
		return find(this.getRoot(), item);
	}

	public TreeItem<MatrixItem> find(TreeItem<MatrixItem> parent, MatrixItem item)
	{
		return find(parent, matrixItem -> Objects.equals(item, matrixItem));
	}

	public TreeItem<MatrixItem> find(Predicate<MatrixItem> strategy)
	{
		return find(this.getRoot(), strategy);
	}

	public TreeItem<MatrixItem> find(TreeItem<MatrixItem> parent, Predicate<MatrixItem> strategy)
	{
		if (strategy.test(parent.getValue()))
		{
			return parent;
		}
		for (TreeItem<MatrixItem> treeItem : parent.getChildren())
		{
			TreeItem<MatrixItem> itemTreeItem = find(treeItem, strategy);
			if (itemTreeItem != null)
			{
				return itemTreeItem;
			}
		}
		return null;
	}

	private void shortCuts(MatrixTreeRow row, final Settings settings)
	{
		setOnKeyPressed(keyEvent -> Common.tryCatch(() -> {
			if (keyEvent.getCode() == KeyCode.ENTER)
			{
				MatrixItem currentItem = currentItem();
				if (currentItem != null)
				{
					GridPane layout = (GridPane) currentItem.getLayout();
					layout.getChildren()
							.stream()
							.filter(n -> n instanceof GridPane)
							.findFirst()
							.map(p -> ((GridPane) p).getChildren().get(0))
							.ifPresent(Common::setFocusedFast);
				}
			}
			else if (SettingsPanel.match(settings, keyEvent, Settings.SHOW_ALL))
			{
				row.showExpressionsResults();
			}
			else if (keyEvent.getCode() == KeyCode.ESCAPE)
			{
				MatrixTreeView.this.requestFocus();
			}
		}, R.MATRIX_TREE_VIEW_ERROR_ON_DO.get()));

		setOnKeyReleased(keyEvent -> Common.tryCatch(() -> {
			if (SettingsPanel.match(settings, keyEvent, Settings.SHOW_ALL))
			{
				row.hideExpressionsResults();
			}
		}, R.MATRIX_TREE_VIEW_ERROR_ON_HIDE.get()));
	}

	private void initTable()
	{
		this.setEditable(true);
		TreeTableColumn<MatrixItem, Integer> numberColumn = new TreeTableColumn<>("#");
		numberColumn.setSortable(false);
		numberColumn.setMinWidth(50);
		numberColumn.setPrefWidth(50);
		numberColumn.setMaxWidth(50);
		numberColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue().getNumber()));
		numberColumn.setCellFactory(p -> new TreeTableCell<MatrixItem, Integer>(){
			@Override
			protected void updateItem(Integer item, boolean empty)
			{
				super.updateItem(item, empty);
				if (item != null && item != -1 && !empty)
				{
					setText("" + item);
				}
				else
				{
					setText(null);
				}
			}
		});

		TreeTableColumn<MatrixItem, MatrixItemState> iconColumn = new TreeTableColumn<>();
		Label iconLabel = new Label("I");
		iconColumn.setGraphic(iconLabel);
		iconLabel.setTooltip(new Tooltip("State column"));

		iconColumn.setSortable(false);
		iconColumn.setMinWidth(23);
		iconColumn.setPrefWidth(23);
		iconColumn.setMaxWidth(23);
		iconColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue().getItemState()));
		iconColumn.setCellFactory(value -> new IconCell());

		TreeTableColumn<MatrixItem, MatrixItem> gridColumn = new TreeTableColumn<>("Actions");
		gridColumn.setSortable(false);
		gridColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
		gridColumn.setCellFactory(param -> new MatrixItemCell());

		TreeTableColumn<MatrixItem, MatrixItem> reportOffColumn = new TreeTableColumn<>();
		Label reportOffLabel = new Label("R");
		reportOffLabel.setTooltip(new Tooltip("Exclude item from report"));
		reportOffColumn.setGraphic(reportOffLabel);

		reportOffColumn.setSortable(false);
		reportOffColumn.setMinWidth(25);
		reportOffColumn.setMaxWidth(25);
		reportOffColumn.setPrefWidth(25);
		reportOffColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
		reportOffColumn.setCellFactory(p -> new TreeTableCell<MatrixItem, MatrixItem>()
		{
			private CheckBox box = new CheckBox();

			private void updateTooltip()
			{
				this.box.setTooltip(new Tooltip(String.format(R.MATRIX_TREE_VIEW_SET_REPORT_ITEM.get(), (this.box.isSelected() ? R.COMMON_ON.get(): R.COMMON_OFF.get()) )));
			}

			@Override
			protected void updateItem(MatrixItem item, boolean empty)
			{
				super.updateItem(item, empty);
				if (item != null && !(item instanceof End))
				{
					box.setSelected(item.isRepOff());
					updateTooltip();
					box.setOnAction(event -> {
						item.setRepOff(box.isSelected());
						matrix.getChangedProperty().accept(true);
						updateTooltip();
						refresh();
					});
					setGraphic(box);
				}
				else
				{
					setGraphic(null);
				}
			}
		});

		TreeTableColumn<MatrixItem, MatrixItem> offColumn = new TreeTableColumn<>();
		Label offLabel = new Label("E");
		offLabel.setTooltip(new Tooltip("Off item from execution"));
		offColumn.setGraphic(offLabel);

		offColumn.setSortable(false);
		offColumn.setMinWidth(25);
		offColumn.setMaxWidth(25);
		offColumn.setPrefWidth(25);
		offColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
		offColumn.setCellFactory(p -> new TreeTableCell<MatrixItem, MatrixItem>()
		{
			private CheckBox box = new CheckBox();

			private void updateTooltip()
			{
				this.box.setTooltip(new Tooltip(String.format(R.MATRIX_TREE_VIEW_SET_ITEM.get(), (this.box.isSelected() ? R.COMMON_ON.get(): R.COMMON_OFF.get()) )));
			}

			@Override
			protected void updateItem(MatrixItem item, boolean empty)
			{
				super.updateItem(item, empty);
				if (item != null && !(item instanceof End))
				{
					box.setSelected(item.isOff());
					updateTooltip();
					box.setOnAction(event -> {
						item.setOff(box.isSelected());
						matrix.getChangedProperty().accept(true);
						updateTooltip();
						refresh();
					});
					setGraphic(box);
				}
				else
				{
					setGraphic(null);
				}
			}
		});
		offColumn.setEditable(true);

		this.treeColumnProperty().set(gridColumn);
		this.getColumns().add(numberColumn);
		this.getColumns().add(reportOffColumn);
		this.getColumns().add(offColumn);
		this.getColumns().add(iconColumn);
		this.getColumns().add(gridColumn);
		gridColumn.setMaxWidth(Double.MAX_VALUE);
		this.setColumnResizePolicy(param -> true);
		gridColumn.prefWidthProperty().bind(this.widthProperty().subtract(numberColumn.getWidth() + iconColumn.getWidth() + offColumn.getWidth() + reportOffColumn.getWidth()).subtract(2));

		ScrollBar vsb = ((CustomTreeTableViewSkin) this.getSkin()).getVSB();
		vsb.visibleProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue && !oldValue)
			{
				gridColumn.prefWidthProperty().bind(this.widthProperty().subtract(numberColumn.getWidth() + iconColumn.getWidth() + offColumn.getWidth() + reportOffColumn.getWidth()).subtract(18));
			}
			else if (!newValue && oldValue)
			{
				gridColumn.prefWidthProperty().bind(this.widthProperty().subtract(numberColumn.getWidth() + iconColumn.getWidth() + offColumn.getWidth() + reportOffColumn.getWidth()).subtract(2));
			}
		});
	}

	public void scrollTo(int index)
	{
		CustomTreeTableViewSkin skin = (CustomTreeTableViewSkin) this.getSkin();
		skin.scrollTo(index);
	}

	private class MatrixTreeViewSkin extends TreeTableViewSkin<MatrixItem>
	{

		public MatrixTreeViewSkin(TreeTableView<MatrixItem> treeView)
		{
			super(treeView);
		}

		public boolean isIndexVisible(int index)
		{
			return flow.getFirstVisibleCell() != null &&
					flow.getLastVisibleCell() != null &&
					flow.getFirstVisibleCell().getIndex() <= index - 1 &&
					flow.getLastVisibleCell().getIndex() >= index + 1;
		}

		public void show(int index)
		{
			flow.show(index);
		}

		public ScrollBar getVSB()
		{
			ScrollBar scrollBar = (ScrollBar) super.queryAccessibleAttribute(AccessibleAttribute.VERTICAL_SCROLLBAR);
			return scrollBar;
		}
	}
}
