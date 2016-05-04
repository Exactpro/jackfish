////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2016, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.tool.newconfig.nodes;

import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.newconfig.ConfigurationFx;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class VariablesTreeNode extends TreeNode
{
	private ConfigurationFx model;
	private TreeItem<TreeNode> variablesTreeNode;

	public VariablesTreeNode(ConfigurationFx model, TreeItem<TreeNode> variablesTreeNode)
	{
		this.model = model;
		this.variablesTreeNode = variablesTreeNode;
	}

	@Override
	public Node getView()
	{
		return new Text("vars");
	}

	@Override
	public Optional<Image> icon()
	{
		return Optional.of(new Image(CssVariables.Icons.VARS_ICON));
	}

	public void display(List<String>  files)
	{
		this.variablesTreeNode.getChildren().clear();
		files.stream().sorted((f1, f2) -> f1.compareTo(f2)).map(file -> new FileTreeNode(new File(file))
		{
			@Override
			public Optional<ContextMenu> contextMenu()
			{
				ContextMenu menu = super.contextMenu().orElse(new ContextMenu());

				MenuItem itemRemoveVars = new MenuItem("Remove vars file", new ImageView(new Image(CssVariables.Icons.REMOVE_PARAMETER_ICON)));
				itemRemoveVars.setOnAction(e -> Common.tryCatch(() -> model.removeVarsFile(file), "Error on remove vars file"));

				MenuItem itemOpenVars = new MenuItem("Open vars file", new ImageView(new Image(CssVariables.Icons.VARS_ICON)));
				itemOpenVars.setOnAction(event -> model.openVariableFile(new File(file)));

				menu.getItems().addAll(itemRemoveVars, itemOpenVars);
				return Optional.of(menu);
			}

			@Override
			public Common.Function onDoubleClickEvent()
			{
				return () -> model.openVariableFile(new File(file));
			}
		}).map(e -> new TreeItem<TreeNode>(e)).forEach(i -> this.variablesTreeNode.getChildren().add(i));
	}
}
