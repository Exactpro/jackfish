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

package com.exactprosystems.jf.tool.settings;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.common.Settings.SettingsValue;
import com.exactprosystems.jf.tool.Common;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsPanel
{

	public static final List<String> otherList = Stream.of(
			Settings.SHOW_ALL_TABS
			, Settings.SEARCH
	).collect(Collectors.toList());

	public static final List<String> docsList = Stream.of(
			Settings.SAVE_DOCUMENT,
			Settings.SAVE_DOCUMENT_AS,
			Settings.UNDO,
			Settings.REDO
	).collect(Collectors.toList());

	public static final List<String> matrixNavigationList = Stream.of(
			Settings.ADD_ITEMS,
			Settings.ALL_PARAMETERS,
			Settings.BREAK_POINT,
			Settings.ADD_PARAMETER,
			Settings.HELP,
			Settings.GO_TO_LINE,
			Settings.SHOW_ALL,
			Settings.DELETE_ITEM,
			Settings.COPY_ITEMS,
			Settings.CUT_ITEMS,
			Settings.PASTE_ITEMS
	).collect(Collectors.toList());

	public static final List<String> matrixActionsList = Stream.of(
			Settings.START_MATRIX,
			Settings.STOP_MATRIX,
			Settings.PAUSE_MATRIX,
			Settings.STEP_MATRIX,
			Settings.SHOW_RESULT,
			Settings.SHOW_WATCH,
			Settings.TRACING,
			Settings.FIND_ON_MATRIX
	).collect(Collectors.toList());


	private Settings settings;
	private SettingsPanelController controller;

	public SettingsPanel(Settings settings) throws IOException
	{
		this.settings = settings;
		this.controller = Common.loadController(getClass().getResource("Settings.fxml"));
		this.controller.init(this);
	}

	public void show()
	{
		displayGit();
		displayMatrix();
		displayColors();
		displayLogs();
		displayShortcuts();
		displayMain();
		displayWizard();
		this.controller.display(R.COMMON_SETTINGS.get());
	}

	private void displayGit()
	{
		List<SettingsValue> values = this.settings.getValues(Settings.GLOBAL_NS, Settings.GIT);
		this.controller.displayGit(values.stream().collect(Collectors.toMap(SettingsValue::getKey, SettingsValue::getValue)));
	}

	private void displayWizard()
	{
		List<SettingsValue> values = this.settings.getValues(Settings.GLOBAL_NS, Settings.WIZARD_NAME);
		this.controller.displayWizard(values.stream().collect(Collectors.toMap(SettingsValue::getKey, SettingsValue::getValue)));
	}

	private void displayColors()
	{
		List<SettingsValue> values = this.settings.getValues(Settings.GLOBAL_NS, Settings.MATRIX_COLORS);
		this.controller.displayColors(values.stream().collect(Collectors.toMap(SettingsValue::getKey, SettingsValue::getValue)));
	}

	private void displayMain()
	{
		this.controller.displayMain(settings.getValues(Settings.GLOBAL_NS, Settings.SETTINGS)
				.stream()
				.collect(Collectors.toMap(SettingsValue::getKey, SettingsValue::getValue)));
	}

	private void displayMatrix()
	{
		this.controller.displayMatrix(
				settings.getValues(Settings.GLOBAL_NS, Settings.MATRIX_NAME)
						.stream()
						.collect(Collectors.toMap(SettingsValue::getKey, SettingsValue::getValue))
		);
	}

	private void displayLogs()
	{
		Collection<SettingsValue> values = settings.getValues(Settings.GLOBAL_NS, Settings.LOGS_NAME);
		Map<String, String> res = new LinkedHashMap<>();
		values.forEach(value -> res.put(value.getKey(), value.getValue()));
		this.controller.displayLogs(res);

	}

	private void displayShortcuts()
	{
		List<SettingsValue> values = settings.getValues(Settings.GLOBAL_NS, Settings.SHORTCUTS_NAME);

		Function<String, String> get = (key) -> values.stream()
				.filter(sv -> sv.getKey().equals(key))
				.map(SettingsValue::getValue)
				.findFirst()
				.orElse(Common.EMPTY);

		Function<List<String>, Map<String, String>> mapFunction = list -> list.stream().collect(Collectors.toMap(k -> k, get));

		Map<String, String> docs = mapFunction.apply(docsList);
		Map<String, String> matrixNav = mapFunction.apply(matrixNavigationList);
		Map<String, String> matrixAct = mapFunction.apply(matrixActionsList);
		Map<String, String> other = mapFunction.apply(otherList);

		this.controller.displayShortcuts(docs, matrixNav, matrixAct, other);
	}

	public void updateSettingsValue(String key, String dialog, String newValue)
	{
		if (newValue.equals(Common.EMPTY))
		{
			this.settings.remove(Settings.GLOBAL_NS, dialog, key);
		}
		else
		{
			this.settings.setValue(Settings.GLOBAL_NS, dialog, key, newValue);
		}
	}

	public void save() throws Exception
	{
		this.settings.saveIfNeeded();
		Settings.SettingsValue theme = this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.THEME);
		Theme.setCurrentTheme(theme.getValue());
	}

	public String nameOtherShortcut(String value, String currentKey)
	{
		return this.settings.getValues(Settings.GLOBAL_NS, Settings.SHORTCUTS_NAME).stream()
				.filter(sv -> sv.getValue().equals(value))
				.map(SettingsValue::getKey)
				.filter(key -> !key.equals(currentKey))
				.findFirst()
				.orElse(null);
	}

	public void removeAll(String dialog)
	{
		this.settings.removeAll(Settings.GLOBAL_NS, dialog);
	}

	public static boolean match(Settings settings, KeyEvent event, String shortcutName)
	{
		List<SettingsValue> values = settings.getValues(Settings.GLOBAL_NS, Settings.SHORTCUTS_NAME);
		Optional<SettingsValue> first = values.stream().filter(value -> value.getKey().equals(shortcutName)).findFirst();
		return first.isPresent() && KeyCodeCombination.valueOf(first.get().getValue()).match(event);
	}

	public static String getShortcutName(Settings settings, String shortcutName)
	{
		SettingsValue value = settings.getValue(Settings.GLOBAL_NS, Settings.SHORTCUTS_NAME, shortcutName);
		if (value == null)
		{
			return "";
		}
		return " <" + value.getValue() + ">";
	}

	private static List<String> createList(String... args)
	{
		List<String> list = new ArrayList<>();
		Arrays.stream(args).forEach(list::add);
		return list;
	}

	public static void setValue(String keyName, Map<String, String> map, Consumer<String> consumer)
	{
		String value = map.get(keyName);
		if (value != null)
		{
			consumer.accept(value);
		}
	}
}