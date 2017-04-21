////////////////////////////////////////////////////////////////////////////////
//Copyright (c) 2009-2015, Exactpro Systems, LLC
//Quality Assurance & Related Development for Innovative Trading Systems.
//All rights reserved.
//This is unpublished, licensed software, confidential and proprietary
//information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.tool.text;

import com.exactprosystems.jf.actions.ActionsList;
import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.common.Settings.SettingsValue;
import com.exactprosystems.jf.common.highlighter.Highlighter;
import com.exactprosystems.jf.documents.matrix.parser.Tokens;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.ContainingParent;
import com.exactprosystems.jf.tool.custom.tab.CustomTab;
import com.exactprosystems.jf.tool.custom.tab.CustomTabPane;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.reactfx.Subscription;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlainTextFxController implements Initializable, ContainingParent
{
	public  BorderPane            mainPane;
	public  ToolBar               toolbar;
	public  Button                btnFindAndReplace;
	public  ComboBox<Highlighter> cbHighlighting;
	public  CheckBox              cbShowLineNumbers;
	private StyleClassedTextArea  textArea;

	private Parent      pane;
	private PlainTextFx model;
	private CustomTab   tab;

	private static final String[] KEYWORDS = Arrays.stream(Tokens.values())
			.map(Enum::name)
			.collect(Collectors.toList())
			.toArray(new String[Tokens.values().length]);

	private static final String KEYWORD_PATTERN       = "#(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PAREN_PATTERN         = "\\(|\\)";
	private static final String BRACE_PATTERN         = "\\{|\\}";
	private static final String BRACKET_PATTERN       = "\\[|\\]";
	private static final String SEMICOLON_PATTERN     = ";";
	private static final String STRING_PATTERN        = "\"([^\"\n])*\"";
	private static final String STRING_PATTERN2       = "'([^\'\n])*\'";
	private static final String COMMENT_PATTERN       = "//.*";
	private static final String ACTIONS_PATTERN       = "\\b(" + String.join("|", Arrays.stream(ActionsList.actions).map(Class::getSimpleName).collect(Collectors.toList())) + ")\\b";

	private static final Pattern PATTERN = Pattern.compile(
			"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
					+ "|(?<PAREN>" + PAREN_PATTERN + ")"
					+ "|(?<BRACE>" + BRACE_PATTERN + ")"
					+ "|(?<BRACKET>" + BRACKET_PATTERN + ")"
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
					+ "|(?<STRING>" + STRING_PATTERN + ")"
					+ "|(?<STRING2>" + STRING_PATTERN2 + ")"
					+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					+ "|(?<ACTIONS>" + ACTIONS_PATTERN + ")"

	);

	private Subscription lastSubscription;

	//region Interface Initializible
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
		this.textArea = new StyleClassedTextArea();
		this.textArea.getUndoManager().forgetHistory();
		this.textArea.getUndoManager().mark();

		// position the caret at the beginning
		this.textArea.selectRange(0, 0);
		this.cbHighlighting.getItems().addAll(Highlighter.values());
		this.cbHighlighting.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			Optional.ofNullable(this.lastSubscription).ifPresent(Subscription::unsubscribe);
			this.textArea.setStyleSpans(0, Common.convertFromList(newValue.getStyles(this.textArea.getText())));
			this.lastSubscription = this.textArea.richChanges()
					.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
					.subscribe(change -> this.textArea.setStyleSpans(0, Common.convertFromList(newValue.getStyles(this.textArea.getText()))));
		});
		this.cbHighlighting.getSelectionModel().selectFirst();
		this.cbShowLineNumbers.selectedProperty().addListener((observable, oldValue, newValue) ->
				this.textArea.setParagraphGraphicFactory(newValue ? LineNumberFactory.get(this.textArea) : null)
		);
		this.mainPane.setCenter(this.textArea);
		GridPane.setColumnSpan(this.textArea, 2);
	}
	//endregion

	//region Interface ContainingParent
	@Override
	public void setParent(Parent parent)
	{
		this.pane = parent;
	}
	//endregion

	public void init(PlainTextFx model, Settings settings)
	{
		this.model = model;
		SettingsValue value = settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.FONT, "Monospaced$16");
		this.textArea.setFont(Common.fontFromString(value.getValue()));
		this.tab = CustomTabPane.getInstance().createTab(model);
		this.tab.setContent(this.pane);
		CustomTabPane.getInstance().addTab(this.tab);
		CustomTabPane.getInstance().selectTab(this.tab);
	}

	public void saved(String name)
	{
		this.tab.saved(name);
	}

	public void close() throws Exception
	{
		this.tab.close();
		CustomTabPane.getInstance().removeTab(this.tab);
	}

	public void displayTitle(String title)
	{
		this.tab.setTitle(title);
	}

	public void displayText(StringProperty property)
	{
		this.textArea.clear();
		this.textArea.appendText(property.get());
		this.textArea.textProperty().addListener((observable, oldValue, newValue) -> property.set(newValue));
	}

	public void findAndReplace(ActionEvent actionEvent)
	{

	}


}
