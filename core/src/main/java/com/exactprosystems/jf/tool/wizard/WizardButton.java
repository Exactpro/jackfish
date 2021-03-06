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

package com.exactprosystems.jf.tool.wizard;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.wizard.Wizard;
import com.exactprosystems.jf.api.wizard.WizardManager;
import com.exactprosystems.jf.common.utils.ArrayUtils;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.function.Supplier;

public class WizardButton extends Button
{
	public static WizardButton smallButton()
	{
		WizardButton button = new WizardButton();
		button.setId("btnSmallWizard");
		return button;
	}

	public static WizardButton normalButton()
	{
		return new WizardButton();
	}

	public static Menu createMenu()
	{
		Menu menu = new Menu(R.WIZARD_WIZARD.get());
		menu.setGraphic(new ImageView(new Image(CssVariables.Icons.WIZARD_SMALL)));
		return menu;
	}

	public WizardButton()
	{
		super();
        this.setTooltip(new Tooltip(R.WIZARDS.get()));
		this.setId("btnWizard");
		this.getStyleClass().addAll(CssVariables.TRANSPARENT_BACKGROUND);
	}

	public void initButton(Context context, WizardManager manager, Supplier<Object[]> criteriesSupplier, Supplier<Object[]> parameterSupplier)
	{
	    this.setOnAction(e ->
	    {
	        Object[] criteries = criteriesSupplier.get();
	        List<Class<? extends Wizard>> suitable  = manager.suitableWizards(criteries);
	        if (suitable.isEmpty())
	        {
	            DialogsHelper.showInfo(R.WIZARD_NO_ONE_ACCESSIBLE.get());
	            return;
	        }
	        Class<? extends Wizard> wizardClass = DialogsHelper.selectFromList(R.WIZARD_CHOOSE_WIZARD.get(), null, suitable, wizard ->  manager.nameOf(wizard));
	        Object[] parameters = criteries;
 	        if (parameterSupplier != null)
	        {
 	           parameters = ArrayUtils.concat(criteries, parameterSupplier.get());
	        }
            manager.runWizard(wizardClass, context, parameters);
	    });
	}
}
