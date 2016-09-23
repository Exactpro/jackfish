////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.gui;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.api.app.ImageWrapper;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

import java.io.File;


@ActionAttribute(
		group					= ActionGroups.GUI,
		suffix					= "IMGRPT",
		generalDescription 		= "Get an image from screen",
		additionFieldsAllowed 	= false,
		outputDescription 		= "Path to image.",
		outputType 				= String.class
	)
public class ImageReport extends AbstractAction
{
	public final static String	imageName	= "Image";
	public final static String beforeTestCaseName = "BeforeTestCase";
	public final static String	titleName	= "Title";
	
	@ActionFieldAttribute(name = imageName, mandatory = true, description = "Image to report.")
	protected ImageWrapper		image		= null;

	@ActionFieldAttribute(name = beforeTestCaseName, mandatory = false, description = "The name of Testcase before witch the picture will be put.")
	protected String 	beforeTestCase 	= null;

	@ActionFieldAttribute(name = titleName, mandatory = false, description = "Title for picture.")
	protected String			title;

    public ImageReport()
	{
	}

	@Override
	public void initDefaultValues() 
	{
		this.beforeTestCase	= null;
		this.title			= null;
	}
    
	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception 
	{
		if (image == null)
		{
			throw new Exception("Image can't be null");
		}
		String outPath = this.image.getFileName();
		if (this.image.getFileName() == null)
		{
			File file = this.image.saveToDir(report.getReportDir());
			outPath = file.getPath();
			report.outImage(super.owner, this.beforeTestCase, file.getName(), this.title);
		}
		else
		{
			report.outImage(super.owner, this.beforeTestCase, this.image.getFileName(), this.title); 
		}
		
		super.setResult(outPath);
	}

}
