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

package com.exactprosystems.jf.tool.newconfig.nodes;

import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.common.SerializablePair;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.documents.config.HandlerKind;
import com.exactprosystems.jf.functions.HelpKind;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.custom.UserInputDialog;
import com.exactprosystems.jf.tool.newconfig.ConfigurationFx;
import com.exactprosystems.jf.tool.newconfig.ConfigurationTreeView;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GlobalHandlerNode extends TreeNode
{
	private ConfigurationFx model;
	private TreeItem<TreeNode> globalHandlerTreeItem;

	private final Text view;
	private final SerializablePair<R, String> CHANGE_HANDLER	= new SerializablePair<>(R.GLOBAL_HANDLER_NODE_CHANGE_HANDLER, null);
	private final SerializablePair<R, String> REMOVE_HANDLER	= new SerializablePair<>(R.GLOBAL_HANDLER_NODE_REMOVE_HANDLER, null);

	public GlobalHandlerNode(ConfigurationFx model, TreeItem<TreeNode> globalHandlerTreeItem)
	{
		this.view = new Text(R.GLOBAL_HANDLER_NODE_VIEW.get());
		this.model = model;
		this.globalHandlerTreeItem = globalHandlerTreeItem;
		this.view.setOpacity(isEnable() ? 1.0 : 0.5);
	}

	@Override
	public Node getView()
	{
		return this.view;
	}

	@Override
	public Optional<Image> icon()
	{
		return Optional.of(new Image(CssVariables.Icons.GLOBAL_HANDLER_ICON));
	}

	@Override
	public Optional<ContextMenu> contextMenu()
	{
		ContextMenu menu = new ContextMenu();

		MenuItem changeEnable = getChangeEnableMenuItem();
		changeEnable.setOnAction(e -> {
			Boolean enabled = this.model.getGlobalHandler().isEnabled();
			this.model.getGlobalHandler().setEnabled(!enabled);
			this.view.setOpacity(isEnable() ? 1.0 : 0.5);
		});
		menu.getItems().addAll(changeEnable, ConfigurationTreeView.createDisabledItem(CHANGE_HANDLER));
		return Optional.of(menu);
	}

	private MenuItem getChangeEnableMenuItem()
	{
		return new MenuItem("Set " + (isEnable() ? "dis" : "en") + "able");
	}

	private boolean isEnable()
	{
		return this.model.getGlobalHandler().isEnabled();
	}

	public void display(Map<HandlerKind, String> map)
	{
		this.globalHandlerTreeItem.getChildren().clear();
		map.entrySet().stream()
				.map(entry -> new TreeNodeHandler(entry.getKey(), entry.getValue()))
				.map(entry -> new TreeItem<TreeNode>(entry))
				.forEach(treeItem -> this.globalHandlerTreeItem.getChildren().add(treeItem));
	}

	private class TreeNodeHandler extends TreeNode
	{
		private HandlerKind kind;
		private String value;

		TreeNodeHandler(HandlerKind kind, String value)
		{
			this.kind = kind;
			this.value = value;
		}

		@Override
		public Node getView()
		{
			return new Text(kind.name() + (Str.IsNullOrEmpty(this.value) ? "" : " : " + this.value));
		}

		@Override
		public Optional<Image> icon()
		{
			return Optional.empty();
		}

		@Override
		public Optional<ContextMenu> contextMenu()
		{
			ContextMenu menu = new ContextMenu();
			MenuItem changeEnable = getChangeEnableMenuItem();
			changeEnable.setDisable(true);

			menu.getItems().addAll(
					changeEnable,
					ConfigurationTreeView.createItem(CHANGE_HANDLER, this::changeHandler, R.GLOBAL_HANDLER_NODE_ERROR_CHANGE.get()),
					ConfigurationTreeView.createItem(REMOVE_HANDLER, this::removeHandler, R.GLOBAL_HANDLER_NODE_ERROR_REMOVE.get())
			);

			return Optional.of(menu);
		}

		private void changeHandler() throws Exception
		{
			List<ReadableValue> list = new ArrayList<>();
			model.addSubcaseFromLibs(list);
			UserInputDialog dialog = new UserInputDialog(
					Str.IsNullOrEmpty(this.value) ? "" : this.value,
					model.createEvaluator(),
					HelpKind.ChooseFromList,
					list,
					-1
			);
			dialog.setTitle(String.format(R.GLOBAL_HANDLER_NODE_CHOOSE_SUBCASE.get(), this.kind.name()));
			Optional<Object> value = dialog.showAndWait();
			value.ifPresent(v -> Common.tryCatch(() -> model.updateHandlerValue(this.kind, Str.asString(v)), R.GLOBAL_HANDLER_NODE_ERROR_UPDATE.get()));
		}

		private void removeHandler()
		{
			Common.tryCatch(() -> model.updateHandlerValue(this.kind, ""), R.GLOBAL_HANDLER_NODE_ERROR_REMOVE.get());
		}

		@Override
		public Common.Function onDoubleClickEvent()
		{
			return this::changeHandler;
		}
	}
}
