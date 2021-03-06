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

package com.exactprosystems.jf.tool.git.tag;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.git.GitUtil;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class GitTagController implements Initializable, ContainingParent
{
	public Parent parent;
	public Button btnDeleteTag;
	public ListView<GitUtil.Tag> listView;
	public Button btnPushTags;
	public Button btnNewTag;
	public Button btnClose;
	private GitTag model;
	private Alert dialog;


	//region Initializable
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{

	}
	//endregion

	//region ContainingParent
	@Override
	public void setParent(Parent parent)
	{
		this.parent = parent;
	}
	//endregion

	void init(GitTag model)
	{
		this.model = model;
		this.dialog = DialogsHelper.createGitDialog(R.GIT_TAG_CONTR_INIT_TITLE.get(), this.parent);
		this.listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				this.btnDeleteTag.setDisable(false);
			}
		});
		this.listView.setCellFactory(p -> new ListCell<GitUtil.Tag>() {
			@Override
			protected void updateItem(GitUtil.Tag item, boolean empty)
			{
				super.updateItem(item, empty);
				if (!empty && item != null)
				{
					setText(item.getName());
				}
				else
				{
					setText(null);
				}
			}
		});
	}

	void show()
	{
		this.dialog.showAndWait();

	}

	public void hide(ActionEvent event)
	{
		this.dialog.hide();
	}

	void updateTags(List<GitUtil.Tag> list)
	{
		this.listView.getItems().setAll(list);
		this.listView.getSelectionModel().selectFirst();
	}

	void setDisable(boolean flag)
	{
		this.listView.setDisable(flag);
		this.btnDeleteTag.setDisable(flag);
		this.btnPushTags.setDisable(flag);
		this.btnNewTag.setDisable(flag);
		this.btnClose.setDisable(flag);
	}

	public void deleteTag(ActionEvent actionEvent)
	{
		Common.tryCatch(() -> this.model.deleteTag(this.listView.getSelectionModel().getSelectedItem().getFullname()), R.GIT_TAG_CONTR_ERROR_ON_DELETE.get());
	}

	public void newTag(ActionEvent actionEvent)
	{
		TextInputDialog versionDialog = new TextInputDialog();
		versionDialog.setTitle(R.GIT_TAG_CONTR_ENTER_VERSION.get());
		versionDialog.getDialogPane().setHeader(new Label());
		Node node = versionDialog.getDialogPane().lookupButton(ButtonType.OK);
		versionDialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				node.setDisable(this.listView.getItems()
						.stream()
						.map(GitUtil.Tag::getName)
						.anyMatch(newValue::equals)
				);
			}
		});
		Optional<String> version = versionDialog.showAndWait();
		if (!version.isPresent())
		{
			return;
		}
		TextInputDialog messageDialog = new TextInputDialog();
		messageDialog.setTitle(R.GIT_TAG_CONTR_ENTER_MESSAGE.get());
		messageDialog.getDialogPane().setHeader(new Label());
		Optional<String> msg = messageDialog.showAndWait();
		if (!msg.isPresent())
		{
			return;
		}
		Common.tryCatch(() -> this.model.newTag(version.get(), msg.get()), R.GIT_TAG_CONTR_ERROR_ON_DELETE.get());
	}

	public void pushTag(ActionEvent actionEvent)
	{
		Common.tryCatch(() -> this.model.pushTag(), R.GIT_TAG_CONTR_ERROR_ON_PUSH.get());
	}
}