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

package com.exactprosystems.jf.tool.settings.tabs;

import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.actions.ActionsList;
import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.documents.matrix.parser.Tokens;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.settings.SettingsPanel;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ColorsTabController implements Initializable, ContainingParent, ITabHeight, ITabRestored
{
	public Parent parent;
	public ColorPicker colorPicker;
	public BorderPane borderView;
	public Button btnDefault;
	public GridPane colorsPane;

	private SettingsPanel model;
	private TreeView<TreeCellBean> treeViewColors;
	private Map<String, Color> colorsMap = new HashMap<>();

	//region Initializable
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		this.treeViewColors = new TreeView<>();
		this.treeViewColors.setRoot(new TreeItem<>(new TreeCellBean("")));
		this.treeViewColors.setShowRoot(false);
		this.treeViewColors.setCellFactory(param -> new CustomTreeCell());
		this.treeViewColors.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				boolean isChangeable = newValue.getValue().isChangeable;
				this.btnDefault.setDisable(!isChangeable);
				this.colorPicker.setDisable(!isChangeable);
				if (isChangeable)
				{
					this.colorPicker.setValue(this.colorsMap.get(newValue.getValue().name));
				}
			}
		});
		this.colorPicker.setOnAction(event ->  {
			TreeItem<TreeCellBean> selectedItem = this.treeViewColors.getSelectionModel().getSelectedItem();
			if (selectedItem != null)
			{
				this.colorsMap.put(selectedItem.getValue().name, this.colorPicker.getValue());
				updateTree();
			}
		});

		initialColorItems();
		restoreToDefault();
	}
	//endregion

	//region ContainingParent
	@Override
	public void setParent(Parent parent)
	{
		this.parent = parent;
	}
	//endregion

	public void init(SettingsPanel model)
	{
		this.model = model;
	}

	//region display methods
	public void displayInfo(Map<String, String> knownColors)
	{
		this.colorsMap.putAll(knownColors.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> Common.stringToColor(entry.getValue()))));
		workWithTree(this.treeViewColors.getRoot(), treeItem -> {
			String name = treeItem.getValue().name;
			this.colorsMap.putIfAbsent(name, Color.TRANSPARENT);
		});
	}

	public void displayInto(Tab tab)
	{
		tab.setContent(this.parent);
		tab.setUserData(this);
	}
	//endregion

	@Override
	public double getHeight()
	{
		return -1;
	}

	public void save()
	{
		this.model.removeAll(Settings.MATRIX_COLORS);
		this.colorsMap.entrySet()
				.stream()
				.filter(e -> !e.getValue().equals(Color.TRANSPARENT))
				.forEach(entry -> this.model.updateSettingsValue(entry.getKey(), Settings.MATRIX_COLORS, Common.colorToString(entry.getValue())));
	}

	@Override
	public void restoreToDefault()
	{
		Settings settings = Settings.defaultSettings();
		this.colorsMap.replaceAll((key,value) -> getDefaultColor(settings, key));
		updateTree();
	}

	public void restoreDefaults(ActionEvent actionEvent)
	{
		restoreToDefault();
	}

	//region event methods
	public void defaultColor(ActionEvent actionEvent)
	{
		TreeItem<TreeCellBean> selectedItem = this.treeViewColors.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
		{
			String name = selectedItem.getValue().name;
			this.colorsMap.replace(name, this.getDefaultColor(Settings.defaultSettings(), name));
			updateTree();
		}
	}

	public void expandAll(ActionEvent actionEvent)
	{
		workWithTree(this.treeViewColors.getRoot(), item -> item.setExpanded(true));
	}

	public void collapseAll(ActionEvent actionEvent)
	{
		this.treeViewColors.getRoot().getChildren().forEach(item -> workWithTree(item, treeItem-> treeItem.setExpanded(false)));
	}

	public void clearAll(ActionEvent actionEvent)
	{
		this.colorsMap.replaceAll((s, color) -> Color.TRANSPARENT);
		updateTree();
	}

	//endregion

	//region private methods
	private Color getDefaultColor(Settings settings, String key)
	{
		return Optional.ofNullable(settings.getValue(Settings.GLOBAL_NS, Settings.MATRIX_COLORS, key))
				.map(Settings.SettingsValue::getValue)
				.map(Color::valueOf)
				.orElse(Color.TRANSPARENT);
	}

	private void updateTree()
	{
		boolean oldExpandedState = this.treeViewColors.getRoot().isExpanded();
		this.treeViewColors.getRoot().setExpanded(!oldExpandedState);
		this.treeViewColors.getRoot().setExpanded(oldExpandedState);
	}

	private void initialColorItems()
	{
		TreeCellBean actions = new TreeCellBean("Actions");
		actions.isChangeable = false;
		TreeItem<TreeCellBean> actionItem = new TreeItem<>(actions);
		this.treeViewColors.getRoot().getChildren().addAll(
				actionItem,
				new TreeItem<>(new TreeCellBean(Tokens.NameSpace.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Step.name())),
				new TreeItem<>(new TreeCellBean(Tokens.TestCase.name())),
				new TreeItem<>(new TreeCellBean(Tokens.SubCase.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Return.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Call.name())),
				new TreeItem<>(new TreeCellBean(Tokens.If.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Else.name())),
				new TreeItem<>(new TreeCellBean(Tokens.For.name())),
				new TreeItem<>(new TreeCellBean(Tokens.ForEach.name())),
				new TreeItem<>(new TreeCellBean(Tokens.While.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Continue.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Break.name())),
				new TreeItem<>(new TreeCellBean(Tokens.OnError.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Switch.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Case.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Default.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Fail.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Let.name())),
				new TreeItem<>(new TreeCellBean(Tokens.Assert.name())),
				new TreeItem<>(new TreeCellBean(Tokens.RawTable.name())),
				new TreeItem<>(new TreeCellBean(Tokens.RawText.name())),
				new TreeItem<>(new TreeCellBean(Tokens.RawMessage.name())),
				new TreeItem<>(new TreeCellBean(Tokens.SetHandler.name()))
		);

		Map<ActionGroups, TreeItem<TreeCellBean>> map = new HashMap<>();
		Arrays.stream(ActionGroups.values()).forEach(group -> {
			TreeCellBean tmp = new TreeCellBean(group.name());
			TreeItem<TreeCellBean> item = new TreeItem<>(tmp);
			actionItem.getChildren().add(item);
			map.put(group, item);
		});
		Arrays.asList(ActionsList.actions).forEach(clazz -> {
			TreeCellBean bean = new TreeCellBean(clazz.getSimpleName());
			ActionGroups aClazz = clazz.getAnnotation(ActionAttribute.class).group();
			map.get(aClazz).getChildren().add(new TreeItem<>(bean));
		});
		this.treeViewColors.getSelectionModel().selectFirst();
		this.borderView.setCenter(this.treeViewColors);
	}

	private void workWithTree(TreeItem<TreeCellBean> root, Consumer<TreeItem<TreeCellBean>> fnc)
	{
		fnc.accept(root);
		root.getChildren().forEach(child -> workWithTree(child, fnc));
	}

	//endregion

	//region private classes
	private static class TreeCellBean
	{
		private String name;
		private boolean isChangeable = true;

		TreeCellBean(String name)
		{
			this.name = name;
		}
	}

	private class CustomTreeCell extends TreeCell<TreeCellBean>
	{
		@Override
		protected void updateItem(TreeCellBean item, boolean empty)
		{
			super.updateItem(item, empty);
			this.getStyleClass().remove(CssVariables.TREE_ITEM_WITH_BORDERS);
			if (item != null && !empty)
			{
				this.getStyleClass().add(CssVariables.TREE_ITEM_WITH_BORDERS);
				BorderPane pane = new BorderPane();
				Label value = new Label(item.name);
				BorderPane.setAlignment(value, Pos.CENTER_LEFT);
				pane.setCenter(value);

				Rectangle rectangle = new Rectangle(16,16);
				rectangle.setFill(colorsMap.get(item.name));

				pane.setRight(rectangle);
//				if (item.isChangeable)
//				{
//					ColorPicker color = new ColorPicker(item.color);
//					color.setOnAction(e -> {
//						item.color = e instanceof ClearAction ? Color.TRANSPARENT : color.getValue();
//						colorsMap.remove(item.name);
//						if (!item.color.equals(Color.TRANSPARENT))
//						{
//							colorsMap.put(item.name, item.color);
//						}
//						Optional.ofNullable(this.getTreeItem().getChildren()).ifPresent(child -> child.forEach(c -> {
//							TreeCellBean value1 = c.getValue();
//							value1.color = item.color;
//							c.setValue(null);
//							c.setValue(value1);
//							colorsMap.put(value1.name, item.color);
//						}));
//						this.updateItem(item, false);
//					});
//					HBox box = new HBox();
//					box.setAlignment(Pos.CENTER);
//					box.setSpacing(10);
//					box.getChildren().add(color);
//					Button reset = new Button("Reset");
//					reset.setOnAction(e -> color.getOnAction().handle(new ClearAction()));
//					pane.setRight(box);
//				}
				setGraphic(pane);
			}
			else
			{
				setGraphic(null);
			}
		}
	}
	//endregion
}