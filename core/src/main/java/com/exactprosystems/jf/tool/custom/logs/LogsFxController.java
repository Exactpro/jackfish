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

package com.exactprosystems.jf.tool.custom.logs;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.custom.find.FindPanel;
import com.exactprosystems.jf.tool.custom.find.IFind;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import com.exactprosystems.jf.tool.settings.Theme;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LogsFxController implements Initializable, ContainingParent
{
	@FXML
	private BorderPane     borderPane;
	@FXML
	private ComboBox<File> cbFiles;
	@FXML
	private VBox           topVBox;

	private InlineCssTextArea               consoleArea;
	private LogsFx                          model;
	private Dialog                          dialog;
	private FindPanel<LogsFx.LineWithStyle> findPanel;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
		this.findPanel = new FindPanel<>();
		this.findPanel.getStyleClass().remove(CssVariables.FIND_PANEL);
		this.topVBox.getChildren().add(this.findPanel);

		this.consoleArea = new InlineCssTextArea();
		this.consoleArea.setParagraphGraphicFactory(LineNumberFactory.get(this.consoleArea));
		this.consoleArea.setEditable(false);
		this.borderPane.setCenter(new VirtualizedScrollPane<>(this.consoleArea));
		BorderPane.setMargin(this.consoleArea, new Insets(10, 0, 0, 0));
	}

	@Override
	public void setParent(Parent parent)
	{
		ButtonType close = new ButtonType(R.COMMON_CLOSE.get(), ButtonBar.ButtonData.OK_DONE);
		this.dialog = new Alert(Alert.AlertType.INFORMATION, "", close);
		DialogsHelper.centreDialog(this.dialog);
		Common.addIcons(((Stage) this.dialog.getDialogPane().getScene().getWindow()));
		this.dialog.setResizable(true);
		this.dialog.getDialogPane().setPrefWidth(600);
		this.dialog.getDialogPane().setPrefHeight(600);
		this.dialog.setTitle(R.LOGS_LOGS.get());
		this.dialog.getDialogPane().setHeader(new Label());
		this.dialog.getDialogPane().setContent(parent);
		this.dialog.getDialogPane().getStylesheets().addAll(Theme.currentThemesPaths());
		this.cbFiles.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				this.model.displayLines(newValue);
			}
		});
	}

	void init(LogsFx model)
	{
		this.model = model;
		this.findPanel.setListener(new IFind<LogsFx.LineWithStyle>()
		{
			@Override
			public void find(LogsFx.LineWithStyle line)
			{
				model.find(line);
			}

			@Override
			public List<LogsFx.LineWithStyle> findItem(String what, boolean matchCase, boolean wholeWord)
			{
				return model.findItem(what, matchCase, wholeWord);
			}
		});
	}

	void show()
	{
		this.dialog.show();
	}

	void displayLines(List<LogsFx.LineWithStyle> list)
	{
		for (LogsFx.LineWithStyle line : list)
		{
			int start = this.consoleArea.getLength();
			this.consoleArea.appendText(line.getLine() + "\n");
			this.consoleArea.setStyle(start, this.consoleArea.getLength(), "-fx-fill: " + Common.colorToString(line.getStyle()));
		}
	}

	void clearListView()
	{
		this.consoleArea.clear();
	}

	void clearFiles()
	{
		this.cbFiles.getItems().clear();
	}

	void displayFiles(List<File> files)
	{
		this.cbFiles.getItems().setAll(files);
		this.cbFiles.getSelectionModel().selectFirst();
	}

	@FXML
	private void refresh(ActionEvent actionEvent)
	{
		this.model.refresh();
	}

	void clearAndSelect(int index)
	{
		this.consoleArea.moveTo(index, 0);
		this.consoleArea.setEstimatedScrollY(this.consoleArea.getTotalHeightEstimate() / this.consoleArea.getParagraphs().size() * index);
	}
}
