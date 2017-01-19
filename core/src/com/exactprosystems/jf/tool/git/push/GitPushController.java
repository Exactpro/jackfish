package com.exactprosystems.jf.tool.git.push;

import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.git.reset.FileWithStatusBean;
import com.exactprosystems.jf.tool.git.reset.GitResetBean;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GitPushController implements Initializable, ContainingParent
{
	public Parent parent;
	public Button btnPush;
	public Button btnClose;
	public ListView<FileWithStatusBean> listViewChanges;
	public ListView<GitResetBean> listViewCommits;

	public Label lblLocalBranch;
	public Label lblPlus;
	public ComboBox<String> cbBranch;

	private GitPush model;
	private Alert dialog;


	//region Initializable
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		this.listViewCommits.setCellFactory(p -> new ListCell<GitResetBean>(){
			@Override
			protected void updateItem(GitResetBean item, boolean empty)
			{
				super.updateItem(item, empty);
				if (!empty && item != null)
				{
					setText(item.getMessage());
				}
				else
				{
					setText(null);
				}
			}
		});

		this.listViewCommits.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				Common.tryCatch(() -> listViewChanges.getItems().setAll(newValue.getFiles()), "Error on display files");
			}
		});

		this.listViewChanges.setCellFactory(p -> new ListCell<FileWithStatusBean>(){
			@Override
			protected void updateItem(FileWithStatusBean item, boolean empty)
			{
				super.updateItem(item, empty);
				if (!empty && item != null)
				{
					Text text = new Text(item.getFile().getPath());
					text.setFill(item.getColor());
					setGraphic(text);
				}
				else
				{
					setGraphic(null);
				}
			}
		});
	}
	//endregion

	//region ContainingParent
	@Override
	public void setParent(Parent parent)
	{
		this.parent = parent;
	}
	//endregion

	void init(GitPush model)
	{
		this.model = model;
		this.dialog = DialogsHelper.createGitDialog("Push commits", this.parent);
	}

	void show()
	{
		this.dialog.show();
		this.listViewCommits.getSelectionModel().selectFirst();
	}

	void hide()
	{
		this.dialog.hide();
	}

	void displayUnpushingCommits(List<GitResetBean> list)
	{
		this.listViewCommits.getItems().setAll(list);
		this.btnPush.setDisable(list.isEmpty());
	}

	void displayCurrentBranch(String currentBranch)
	{
		this.lblLocalBranch.setText(currentBranch);
	}

	void displayRemoteBranch(List<String> branches, String remoteBranch)
	{
		this.cbBranch.getItems().setAll(branches);
		this.cbBranch.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			this.lblPlus.setText(" ");
			if (!cbBranch.getItems().contains(newValue))
			{
				this.lblPlus.setText("+");
			}
		});
		if (remoteBranch != null)
		{
			this.cbBranch.getEditor().setText(remoteBranch);
		}
		else
		{
			this.cbBranch.getSelectionModel().select("master");
		}
	}

	void setDisable(boolean flag)
	{
		this.btnClose.setText(flag ? "Cancel" : "Close");
		this.btnPush.setDisable(flag);
		this.cbBranch.setDisable(flag);
		this.listViewChanges.setDisable(flag);
		this.listViewCommits.setDisable(flag);
	}

	public void close(ActionEvent event)
	{
		Common.tryCatch(this.model::close, "Error on close cancel");
	}


	public void push(ActionEvent actionEvent)
	{
		Common.tryCatch(() -> this.model.push(this.cbBranch.getEditor().getText()), "Error on push");
	}
}