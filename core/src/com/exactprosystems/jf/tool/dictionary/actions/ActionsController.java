////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.tool.dictionary.actions;

import com.exactprosystems.jf.api.app.ImageWrapper;
import com.exactprosystems.jf.api.app.Operation;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.css.images.Images;
import com.exactprosystems.jf.tool.custom.expfield.ExpressionField;
import com.exactprosystems.jf.tool.dictionary.ApplicationStatus;
import com.exactprosystems.jf.tool.dictionary.DictionaryFx;
import com.exactprosystems.jf.tool.dictionary.info.element.ElementInfoController;
import com.exactprosystems.jf.tool.dictionary.navigation.NavigationController;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;


import javax.imageio.ImageIO;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;


import static com.exactprosystems.jf.tool.Common.*;

public class ActionsController implements Initializable, ContainingParent
{
	public Label					imageArea;
	public GridPane					elementActionsGrid;
	
	public ComboBox<String>			comboBoxApps;
	public Button					btnStartApplication;
	public Button					btnConnectApplication;
	public Button					btnStop;
	public TextField				tfSendKeys;
	public ComboBox<String>			comboBoxWindows;

	public GridPane					mainGrid;
	private ExpressionField			expressionField;
	private Parent					pane;

	private DictionaryFx			model;
	private NavigationController 	navigation; 
	private ElementInfoController 	info;

	@Override
	public void setParent(Parent parent)
	{
		this.pane = parent;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
		assert btnStop != null : "fx:id=\"btnStopConnection\" was not injected: check your FXML file 'Actions.fxml'.";
		assert mainGrid != null : "fx:id=\"mainGrid\" was not injected: check your FXML file 'Actions.fxml'.";
		assert comboBoxWindows != null : "fx:id=\"comboBoxDriverWindows\" was not injected: check your FXML file 'Actions.fxml'.";
		assert tfSendKeys != null : "fx:id=\"tfSendKeys\" was not injected: check your FXML file 'Actions.fxml'.";
		assert elementActionsGrid != null : "fx:id=\"elementActionsGrid\" was not injected: check your FXML file 'Actions.fxml'.";
		assert comboBoxApps != null : "fx:id=\"comboBoxApps\" was not injected: check your FXML file 'Actions.fxml'.";
		assert btnStartApplication != null : "fx:id=\"btnStartApplication\" was not injected: check your FXML file 'Actions.fxml'.";
		assert btnConnectApplication != null : "fx:id=\"btnConnectApplication\" was not injected: check your FXML file 'Actions.fxml'.";
		assert imageArea != null : "fx:id=\"labelArea\" was not injected: check your FXML file 'Actions.fxml'.";
		String imageText = Images.class.getResource("texture.png").toExternalForm();
		imageArea.setStyle("-fx-background-image:url('" + imageText + "');\n" + "    -fx-background-repeat: repeat;");
	}

	public void init(DictionaryFx model, GridPane gridPane, AbstractEvaluator evaluator, NavigationController navigation, 
			ElementInfoController info)
	{
		this.model = model;
		this.navigation = navigation;
		this.info = info;

		this.expressionField = new ExpressionField(evaluator);
		this.elementActionsGrid.add(this.expressionField, 1, 2, 2, 1);
		this.expressionField.setHelperForExpressionField(null, null);

		this.comboBoxApps.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> 
		{
			this.info.setAppName(newValue);
		}); 

		gridPane.add(this.pane, 0, 2);
		GridPane.setColumnSpan(this.pane, 2);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Event handlers
	// ------------------------------------------------------------------------------------------------------------------
	public void sendKeysAction(ActionEvent actionEvent)
	{
		tryCatch(() -> this.navigation.sendKeys(this.tfSendKeys.getText()), "Error on send keys");
	}

	public void clickAction(ActionEvent actionEvent)
	{
		tryCatch(() -> this.navigation.click(), "Error on click");
	}

	public void findAction(ActionEvent actionEvent)
	{
		tryCatch(() -> this.navigation.find(), "Error on find");
	}

	public void operate(ActionEvent actionEvent)
	{
		tryCatch(() -> this.navigation.operate((Operation) this.expressionField.getEvaluatedValue()), "Error on operate");
	}
	
	
	public void changeWindow(ActionEvent actionEvent)
	{
		tryCatch(() -> this.model.switchTo(currentWindow()), "Error on switch window");
	}

	public void refresh(ActionEvent actionEvent)
	{
		tryCatch(() -> this.model.refresh(), "Error on refresh");
	}

	public void startApplication(ActionEvent actionEvent)
	{
		tryCatch(() -> this.model.startApplication(currentApp()), "Error on start application");
	}

	public void connectApplication(ActionEvent actionEvent)
	{
		tryCatch(() -> this.model.connectToApplication(currentApp()), "Error on connect application");
	}

	public void stopConnection(ActionEvent actionEvent)
	{
		tryCatch(() -> this.model.stopApplication(), "Error on stop application");
	}

	// ------------------------------------------------------------------------------------------------------------------
	// display* methods
	// ------------------------------------------------------------------------------------------------------------------
	public void displayImage(ImageWrapper imageWrapper)
	{
		Platform.runLater(() ->
		{
			tryCatch(() ->
			{
				ImageView imageView = null;
				if (imageWrapper != null)
				{
					BufferedImage image = imageWrapper.getImage();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ImageIO.write(image, "jpg", outputStream);
					Image imageFX = new Image(new ByteArrayInputStream(outputStream.toByteArray()));
					imageView = new ImageView();
					imageView.setImage(imageFX);
				}
				this.imageArea.setGraphic(imageView);
			}, "Error on display image");
		});
	}

	public void displayApplicationStatus(ApplicationStatus status, Throwable ifTrouble)
	{
		Platform.runLater(() ->
		{
			if (status != null)
			{
				switch (status)
				{
					case Disconnected:
						progressBarVisible(false);
						this.comboBoxApps.setDisable(false);
						this.btnStartApplication.setDisable(false);
						this.btnConnectApplication.setDisable(false);
						this.btnStop.setDisable(true);
						break;
		
					case Connecting:
						progressBarVisible(true);
						this.comboBoxApps.setDisable(true);
						this.btnStartApplication.setDisable(true);
						this.btnConnectApplication.setDisable(true);
						this.btnStop.setDisable(true);
						break;
		
					case Connected:
						progressBarVisible(false);
						this.comboBoxApps.setDisable(true);
						this.btnStartApplication.setDisable(true);
						this.btnConnectApplication.setDisable(true);
						this.btnStop.setDisable(false);
						break;
				}
			}
		});
		
		if (ifTrouble != null)
		{
			logger.error(ifTrouble.getMessage(), ifTrouble);
			DialogsHelper.showError(ifTrouble.getMessage());
		}
	}

	public void displayActionControl(Collection<String> entries, String entry, Collection<String> titles, String title)
	{
		Platform.runLater(() ->
		{
			if (entries != null)
			{
				this.comboBoxApps.getItems().setAll(entries);
			}
			this.comboBoxApps.getSelectionModel().select(entry);
			
			if (titles != null)
			{
				this.comboBoxWindows.getItems().setAll(titles);
			}
			/*
				//TODO
				this is need, because if title is null, that listener try to change value to null.
				After that title will be go in remoteApplication and in method switchTo will be throw Exception, because title == null.
				And if the first element is null, i not think about it.
			 */
			if (title != null)
			{
				this.comboBoxWindows.getSelectionModel().select(title);
			}
			else
			{
				this.comboBoxWindows.getSelectionModel().selectFirst();
			}

		});
	}

	// ------------------------------------------------------------------------------------------------------------------
	private String currentApp()
	{
		return this.comboBoxApps.getSelectionModel().getSelectedItem();
	}

	private String currentWindow()
	{
		return this.comboBoxWindows.getSelectionModel().getSelectedItem();
	}
}
