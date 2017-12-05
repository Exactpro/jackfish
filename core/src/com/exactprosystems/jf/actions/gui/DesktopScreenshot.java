////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.gui;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.app.ImageWrapper;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;


@ActionAttribute(
		group					   = ActionGroups.GUI,
		suffix					   = "DSK",
		constantGeneralDescription = R.DESKTOP_SCREENSHOT_GENERAL_DESC,
		additionFieldsAllowed 	   = false,
		constantOutputDescription  = R.DESKTOP_SCREENSHOT_OUTPUT_DESC,
		outputType 				   = ImageWrapper.class,
		constantExamples 		   = R.DESKTOP_SCREENSHOT_EXAMPLE
	)
public class DesktopScreenshot extends AbstractAction
{
	public static final String descriptionName = "Description";

	@ActionFieldAttribute(name = descriptionName, mandatory = false, def = DefaultValuePool.EmptyString, constantDescription = R.DESKTOP_SCREENSHOT_DESCRIPTION_PARAM)
	protected String description;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		Rectangle desktopRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		BufferedImage image = new java.awt.Robot().createScreenCapture(desktopRect);

		ImageWrapper imageWrapper = new ImageWrapper(image);

		if (!Str.IsNullOrEmpty(this.description))
		{
			imageWrapper.setDescription(this.description);
		}
		super.setResult(imageWrapper);
	}
}
