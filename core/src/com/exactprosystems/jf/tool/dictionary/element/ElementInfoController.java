////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.tool.dictionary.element;

import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.custom.BorderWrapper;
import com.exactprosystems.jf.tool.custom.controls.field.CustomFieldWithButton;
import com.exactprosystems.jf.tool.dictionary.DictionaryFx;
import com.exactprosystems.jf.tool.dictionary.navigation.NavigationController;
import com.exactprosystems.jf.tool.settings.Theme;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static com.exactprosystems.jf.tool.Common.get;
import static com.exactprosystems.jf.tool.Common.tryCatch;

public class ElementInfoController implements Initializable, ContainingParent
{
	public ComboBox<ControlKind> comboBoxControl;
	public ChoiceBox<Visibility> choiceBoxVisibility;
	public ChoiceBox<Addition> choiceBoxAddition;
	public ChoiceBox<IControl> choiceBoxHeader;
	public ChoiceBox<IControl> choiceBoxRows;
	public ChoiceBox<IControl> choiceBoxOwner;
	public ChoiceBox<IControl> choiceBoxReference;
	public CheckBox checkBoxWeak;
	public CustomFieldWithButton tfID;
	public CustomFieldWithButton tfUID;
	public CustomFieldWithButton tfXpath;
	public CustomFieldWithButton tfClass;
	public CustomFieldWithButton tfText;
	public CustomFieldWithButton tfName;
	public CustomFieldWithButton tfTooltip;
	public CustomFieldWithButton tfColumns;
	public CustomFieldWithButton tfAction;
	public CustomFieldWithButton tfTitle;
	public CustomFieldWithButton tfExpression;
	public TextField tfTimeout;
	public GridPane mainGrid;
	public Button btnGoToOwner;
	public GridPane fieldGrid;
	public GridPane identifiersGrid;


	private Parent pane;
	private NavigationController navigation;
	private DictionaryFx model;
	private Configuration configuration;
	private ObservableList<ControlKind> controls;
	private IControl currentControl;
	private IWindow window;

	@Override
	public void setParent(Parent parent)
	{
		this.pane = parent;
		Common.runLater(() -> ((BorderPane) this.pane).setCenter(BorderWrapper.wrap(this.mainGrid).color(Theme.currentTheme().getReverseColor()).title("Element info").build()));
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
		Arrays.asList(tfID, tfUID, tfXpath, tfClass, tfText, tfName, tfTooltip, tfColumns, tfAction, tfTitle, tfExpression).forEach(tf -> {
			tf.prefWidthProperty().bind(this.fieldGrid.getColumnConstraints().get(1).maxWidthProperty());
			tf.maxWidthProperty().bind(this.fieldGrid.getColumnConstraints().get(1).maxWidthProperty());
			tf.minWidthProperty().bind(this.fieldGrid.getColumnConstraints().get(1).minWidthProperty());
		});
	}

	public void init(DictionaryFx model, Configuration configuration, GridPane gridPane, NavigationController navigation)
	{
		this.navigation = navigation;
		this.model = model;
		this.configuration = configuration;

		setTextFieldListeners();
		
		setItems(this.choiceBoxAddition, Addition.values());
		setItems(this.choiceBoxVisibility, Visibility.values());
		
		this.controls = FXCollections.observableArrayList(ControlKind.values());
		this.comboBoxControl.setItems(this.controls);

		gridPane.add(this.pane, 1, 0);
	}

	public void setAppName(String id)
	{
		update(this.comboBoxControl.getSelectionModel().selectedItemProperty(),
				() ->
				{
					ControlKind temp = currentControlKind();
					if (id == null)
					{
						this.controls.clear();
						this.controls.addAll(ControlKind.values());
					} else
					{
						tryCatch(
								() ->
								{
									this.controls.clear();
									this.controls.addAll(this.configuration.getApplicationPool()
											.loadApplicationFactory(id)
											.supportedControlKinds());
								}, "Error on set supported controls");
					}
					this.comboBoxControl.setValue(temp);
//				}, this.changeControlKindListener);
				}, null);
	}
	
	//============================================================
	// events methods
	//============================================================
	public void goToOwner(ActionEvent actionEvent)
	{
//		tryCatch(() -> this.navigation.parameterGoToOwner(currentOwner()), "Error on go to the owner");
	}

	public void changeWeak(ActionEvent actionEvent)
	{
//		tryCatch(() -> this.navigation.parameterSet(AbstractControl.weakName, this.checkBoxWeak.isSelected()), "Error on changing weakness");
	}
	
	public void clear(ActionEvent actionEvent)
	{
		tryCatch(() -> {
			String id = ((Node) actionEvent.getSource()).getId();
			TextField tf = (TextField) this.pane.lookup("#" + id);
			tf.clear();
//			this.navigation.parameterSet(id, null);
		}, "Error on clearing " + actionEvent); 
	}

	// ------------------------------------------------------------------------------------------------------------------
	// display* methods
	// ------------------------------------------------------------------------------------------------------------------
	public void displayInfo(IWindow window, IControl control, Collection<IControl> owners, IControl owner, Collection<IControl> rows, IControl row, IControl header, IControl reference)
	{
    	Common.runLater(() ->
		{
			this.pane.setDisable(control == null);

//			update(
//					this.choiceBoxOwner.getSelectionModel().selectedItemProperty(),
//					() -> {
//						this.choiceBoxOwner.getItems().clear();
//						this.choiceBoxOwner.getItems().add(null);
//						if (owners != null)
//						{
//							this.choiceBoxOwner.getItems().addAll(owners);
//						}
//						this.choiceBoxOwner.getSelectionModel().select(owner);
//					},
//					this.changeOwnerListener
//			);
//
//			update(
//					this.choiceBoxReference.getSelectionModel().selectedItemProperty(),
//					() -> {
//						this.choiceBoxReference.getItems().clear();
//						this.choiceBoxReference.getItems().add(null);
//						if (owners != null)
//						{
//							this.choiceBoxReference.getItems().addAll(owners);
//						}
//						this.choiceBoxReference.getSelectionModel().select(reference);
//					},
//					this.changeReferenceListener
//			);
//
//			update(
//					this.choiceBoxHeader.getSelectionModel().selectedItemProperty(),
//					() -> {
//						this.choiceBoxHeader.getItems().clear();
//						this.choiceBoxHeader.getItems().add(null);
//						if (rows != null)
//						{
//							this.choiceBoxHeader.getItems().addAll(rows);
//						}
//						this.choiceBoxHeader.getSelectionModel().select(header);
//					},
//					this.changeHeaderListener
//			);
//			update(
//					this.choiceBoxRows.getSelectionModel().selectedItemProperty(),
//					() -> {
//						this.choiceBoxRows.getItems().clear();
//						this.choiceBoxRows.getItems().add(null);
//						if (rows != null)
//						{
//							this.choiceBoxRows.getItems().addAll(rows);
//						}
//						this.choiceBoxRows.getSelectionModel().select(row);
//					},
//					this.changeRowsListener
//			);
//			update(
//					this.comboBoxControl.getSelectionModel().selectedItemProperty(),
//					() -> this.comboBoxControl.getSelectionModel().select(control == null ? null : control.getBindedClass()),
//					this.changeControlKindListener
//			);
//			update(
//					this.choiceBoxAddition.getSelectionModel().selectedItemProperty(),
//					() -> this.choiceBoxAddition.getSelectionModel().select(control == null ? null : control.getAddition()),
//					this.changeAdditionListener
//			);
//
//			update(
//					this.choiceBoxVisibility.getSelectionModel().selectedItemProperty(),
//					() -> this.choiceBoxVisibility.getSelectionModel().select(control == null ? null : control.getVisibility()),
//					this.changeVisibilityListener
//			);
			this.tfID.setText(get(control, "", IControl::getID));
			this.tfUID.setText(get(control, "", IControl::getUID));
			this.tfXpath.setText(get(control, "", IControl::getXpath));
			this.tfText.setText(get(control, "", IControl::getText));
			this.tfClass.setText(get(control, "", IControl::getClazz));
			this.tfName.setText(get(control, "", IControl::getName));
			this.tfTitle.setText(get(control, "", IControl::getTitle));
			this.tfAction.setText(get(control, "", IControl::getAction));
			this.tfTooltip.setText(get(control, "", IControl::getTooltip));
			this.tfColumns.setText(get(control, "", IControl::getColumns));
			this.tfExpression.setText(get(control, "", IControl::getExpression));
			this.tfTimeout.setText(get(control, "0", IControl::getTimeout));

			this.checkBoxWeak.setSelected(control != null && control.isWeak());
			this.previousValue = "";
			this.currentControl = control;
			this.window = window;
		});
	}

	public void displayElement(IControl control)
	{
		this.pane.setDisable(control == null);
		if (control != null)
		{

		}
	}

	//============================================================
	// private methods
	//============================================================
	private void setTextFieldListeners()
	{
		Stream.of(this.tfUID, this.tfXpath, this.tfClass, this.tfText, this.tfName, this.tfTooltip, this.tfColumns, this.tfAction, this.tfTitle, this.tfExpression)
				.forEach(tf -> {
					tf.focusedProperty().addListener(textFocusListener(tf));
					tf.setHandler(event -> {
						tf.clear();
						changeText(tf, "");
					});
				});
		
		this.tfID.focusedProperty().addListener(textIdFocusListener(this.tfID));
		this.tfID.setHandler(event -> {
			this.tfID.clear();
			changeText(this.tfID, "");
		});
		this.tfTimeout.focusedProperty().addListener(numberFocusListener(this.tfTimeout));
	}

	private void refreshElement()
	{
//		tryCatch(() -> this.navigation.displayElementWithoutInfo(this.window), "Error on displaying element");
	}

	private void changeText(TextField source, String value)
	{
		tryCatch(() -> {
			if (!Str.areEqual(value, previousValue))
			{
//				this.navigation.parameterSet(source.getId(), value, this.currentControl);
				previousValue = value;
			}
		}, "Error on changing " + source.getId());
	}

	private void changeNumber(TextField source, String value)
	{
		tryCatch(() -> {
			if (!Str.areEqual(value, previousValue))
			{
//				this.navigation.parameterSet(source.getId(), Integer.parseInt(value), this.currentControl);
				previousValue = value;
			}
		}, "Error on changing " + source.getId());
	}

	private static <T> void update(ObservableValue<T> property, Common.Function fn, ChangeListener<T> listener)
	{
		Common.tryCatch(() -> 
		{
			property.removeListener(listener);
			fn.call();
			property.addListener(listener);
		}, "Error on update listener");
	}

	private IControl currentOwner()
	{
		return this.choiceBoxOwner.getSelectionModel().getSelectedItem();
	}

	private ControlKind currentControlKind()
	{
		return this.comboBoxControl.getSelectionModel().getSelectedItem();
	}

	@SafeVarargs
	private final <T> void setItems(ChoiceBox<T> cb, T... values)
	{
		cb.getItems().add(null);
		cb.getItems().addAll(values);
	}

	//change listeners
//	private ChangeListener<ControlKind> changeControlKindListener = (observable, oldValue, newValue) ->
//			tryCatch(() -> navigation.parameterSetControlKind(newValue), "Error on changing control kind");
//
//	private ChangeListener<IControl> changeOwnerListener = (observableValue, oldValue, newValue) ->
//			tryCatch(() -> navigation.parameterSetOwner(newValue == null ? null : newValue.getID()), "Error on changing owner");
//
//	private ChangeListener<IControl> changeReferenceListener = (observableValue, oldValue, newValue) ->
//			tryCatch(() -> navigation.parameterSetRef(newValue == null ? null : newValue.getID()), "Error on changing owner");
//
//	private ChangeListener<IControl> changeHeaderListener = ((observable, oldValue, newValue) ->
//			tryCatch(() -> this.navigation.parameterSet(AbstractControl.headerName, newValue == null ? null : newValue.getID()), "Error on changing header"));
//
//	private ChangeListener<IControl> changeRowsListener = ((observable, oldValue, newValue) ->
//			tryCatch(() -> this.navigation.parameterSet(AbstractControl.rowsName, newValue == null ? null : newValue.getID()), "Error on changing rows"));
//
//	private ChangeListener<Addition> changeAdditionListener = ((observable, oldValue, newValue) ->
//			tryCatch(() -> this.navigation.parameterSet(AbstractControl.additionName, newValue), "Error on changing addition") );
//
//	private ChangeListener<Visibility> changeVisibilityListener = ((observable, oldValue, newValue) ->
//			tryCatch(() -> this.navigation.parameterSet(AbstractControl.visibilityName, newValue), "Error on changing visibility") );

	private String previousValue = "";

	private ChangeListener<Boolean> textFocusListener(TextField tf)
	{
		return ((observable, oldValue, newValue) -> {
			if (!oldValue && newValue)
			{
				this.previousValue = tf.getText();
			}
			if (!newValue && oldValue)
			{
				changeText(tf, tf.getText());
			}
		});
	}

	private ChangeListener<Boolean> textIdFocusListener(TextField tf)
	{
		return ((observable, oldValue, newValue) -> {
			if (!oldValue && newValue)
			{
				this.previousValue = tf.getText();
			}
			if (!newValue && oldValue)
			{
			    String id = tf.getText();
//			    if (this.navigation.checkNewId(id))
//			    {
//    				changeText(tf, id);
//    				refreshElement();
//			    }
//			    else
//			    {
//	                DialogsHelper.showError("Element with id " + id + " already exists.");
//	                tf.setText(this.previousValue);
//			    }
			}
		});
	}

	private ChangeListener<Boolean> numberFocusListener(TextField tf)
	{
		return ((observable, oldValue, newValue) -> {
			if (!oldValue && newValue)
			{
				this.previousValue = tf.getText();
			}
			if (!newValue && oldValue)
			{
				changeNumber(tf, tf.getText());
			}
		});
	}

}
