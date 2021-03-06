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

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.documents.DocumentKind;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.newconfig.ConfigurationFx;
import com.exactprosystems.jf.tool.newconfig.ConfigurationTreeView;
import com.exactprosystems.jf.tool.newconfig.TablePair;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigurationTreeNode extends TreeNode
{
	private ConfigurationFx model;

	public ConfigurationTreeNode(ConfigurationFx model)
	{
		this.model = model;
	}

	@Override
	public Optional<ContextMenu> contextMenu()
	{
		ContextMenu menu = new ContextMenu();

		MenuItem refresh = new MenuItem(R.CONF_TREE_NODE_REFRESH.get(), new ImageView(new Image(CssVariables.Icons.REFRESH)));
		refresh.setOnAction(e -> Common.tryCatch(() -> model.refresh(), R.CONF_TREE_NODE_ERROR_ON_REFRESH_CONF.get()));

		Menu menuNew = new Menu(R.COMMON_NEW.get());
		menuNew.getItems().addAll(
				createNewDocumentMenuItem(R.COMMON_DICTIONARY.get(), DocumentKind.GUI_DICTIONARY),
				createNewDocumentMenuItem(R.CONF_TREE_NODE_SYSTEM_VARS.get(), DocumentKind.SYSTEM_VARS),
				createNewDocumentMenuItem(R.COMMON_MATRIX.get(), DocumentKind.MATRIX),
				createNewDocumentMenuItem(R.CONF_TREE_NODE_LIBRARY.get(), () -> this.model.newLibrary()),
				createNewDocumentMenuItem(R.CONF_TREE_NODE_PLAIN_TEXT.get(), DocumentKind.PLAIN_TEXT),
				createNewDocumentMenuItem(R.COMMON_CSV.get(), DocumentKind.CSV)
		);
		MenuItem newDictionary = new MenuItem(R.COMMON_DICTIONARY.get());
		newDictionary.setOnAction(e -> Common.tryCatch(() -> this.model.newDocument(DocumentKind.GUI_DICTIONARY), R.CONF_TREE_NODE_ERROR_ON_CREATE_DIC.get()));

		menu.getItems().add(menuNew);

		menu.getItems().addAll(refresh, new SeparatorMenuItem());
		menu.getItems().addAll(ConfigurationTreeView.gitContextMenu(new File(".")).getItems());
		return Optional.of(menu);
	}

	@Override
	public Node getView()
	{
		HBox box = new HBox();
		String name = this.model.getNameProperty().get();
		String fullPath = ConfigurationFx.path(name);
		if (name.equals(fullPath))
		{
			name = new File(name).getName();
		}
		Text configName = new Text();
		configName.setText(name);
		box.getChildren().add(configName);
		Label lblFullPath = new Label(" (" + fullPath + ")");
		lblFullPath.setTooltip(new Tooltip(fullPath));
		lblFullPath.getStyleClass().add(CssVariables.FULL_PATH_LABEL);
		box.getChildren().add(lblFullPath);
		return box;
	}

	@Override
	public List<TablePair> getParameters()
	{
		List<TablePair> list = new ArrayList<>();
		list.add(TablePair.TablePairBuilder.create(Configuration.version, this.model.getVersionStr()).edit(true).build());
		list.add(TablePair.TablePairBuilder.create(Configuration.matrix, this.model.matrixToString()).tooltipSeparator(ConfigurationFx.SEPARATOR).edit(false).build());
		list.add(TablePair.TablePairBuilder.create(Configuration.library, this.model.libraryToString()).tooltipSeparator(ConfigurationFx.SEPARATOR).edit(false).build());
		list.add(TablePair.TablePairBuilder.create("appDictionaries", this.model.getAppDictionaries()).tooltipSeparator(ConfigurationFx.SEPARATOR).edit(false).build());
		list.add(TablePair.TablePairBuilder.create("clientDictionaries", this.model.getClientDictionaries()).tooltipSeparator(ConfigurationFx.SEPARATOR).edit(false).build());
		return list;
	}

	@Override
	public void updateParameter(String key, String value)
	{
		if (key.equals(Configuration.version))
		{
			this.model.getVersion().set(value);
		}
	}

	@Override
	public Optional<Image> icon()
	{
		return Optional.of(new Image(CssVariables.Icons.CONFIGURATION_ICON));
	}

	private MenuItem createNewDocumentMenuItem(String name, DocumentKind kind)
	{
		MenuItem menuItem = new MenuItem(name);
		menuItem.setOnAction(e -> Common.tryCatch(() -> this.model.newDocument(kind), String.format(R.CONF_TREE_NODE_ERROR_ON_CREATE_NEW.get(), name.toLowerCase())));
		return menuItem;
	}

	private MenuItem createNewDocumentMenuItem(String name, Common.Function fn)
	{
		MenuItem menuItem = new MenuItem(name);
		menuItem.setOnAction(e -> Common.tryCatch(fn, String.format(R.CONF_TREE_NODE_ERROR_ON_CREATE_NEW.get(), name.toLowerCase())));
		return menuItem;
	}
}
