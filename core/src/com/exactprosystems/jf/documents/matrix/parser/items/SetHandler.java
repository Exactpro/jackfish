////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.config.HandlerKind;
import com.exactprosystems.jf.documents.matrix.parser.*;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@MatrixItemAttribute(
		description 	= "Sets up an event handler for all TestCases or Steps.", 
		shouldContain 	= { Tokens.SetHandler },
		mayContain 		= { Tokens.Off, Tokens.Kind },
		real			= true,
		hasValue 		= true, 
		hasParameters 	= false,
        hasChildren 	= false
	)
public final class SetHandler extends MatrixItem 
{	
	public SetHandler()
	{
		super();
		this.name = new MutableValue<String>();
        this.kind = new MutableValue<String>();
	}

	@Override
	public MatrixItem clone() throws CloneNotSupportedException
	{
		SetHandler setHandler = ((SetHandler) super.clone());
		setHandler.name = this.name.clone();
        setHandler.kind = this.kind.clone();
		return setHandler;
	}

	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 3);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.SetHandler.get(), context.getFactory().getSettings());
		driver.showExpressionField(this, layout, 1, 1, Tokens.SetHandler.get(), this.name, this.name,
			(str) -> 
			{
				String res = DialogsHelper.selectFromList("Choose sub case from list", new ReadableValue(str), context.subcases(this)).getValue();
				return res;
			},
			(str) -> 
			{ 
				driver.setCurrentItem(context.referenceToSubcase(str, this));
				return str;
			}, null, 'G' );
		
		driver.showLabel(this, layout, 1, 2, Tokens.Kind.get());
        driver.showComboBox(this, layout, 1, 3, this.kind, this.kind, v ->
        {
            return Arrays.stream(HandlerKind.values()).map(k -> k.toString()).collect(Collectors.toList());
        });

		return layout;
	}

	//==============================================================================================
	// Interface Mutable
	//==============================================================================================
    @Override
    public boolean isChanged()
    {
    	if (this.name.isChanged())
    	{
    		return true;
    	}
    	return super.isChanged();
    }

    @Override
    public void saved()
    {
    	super.saved();
    	this.name.saved();
    }
	
	//==============================================================================================
	// Protected members should be overridden
	//==============================================================================================
	@Override
	public String getItemName()
	{
		return super.getItemName() + " " + this.name + " " + this.kind;
	}

	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters)
	{
		this.name.set(systemParameters.get(Tokens.SetHandler)); 
		this.kind.set(systemParameters.get(Tokens.Kind)); 
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		addParameter(firstLine, secondLine, Tokens.SetHandler.get(), this.name.get());
        addParameter(firstLine, secondLine, Tokens.Kind.get(), this.kind.get());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return 
                SearchHelper.matches(Tokens.SetHandler.get(), what, caseSensitive, wholeWord) ||
                SearchHelper.matches(Tokens.Kind.get(), what, caseSensitive, wholeWord) ||
                SearchHelper.matches(this.name.get(), what, caseSensitive, wholeWord) ||
		        SearchHelper.matches(this.kind.get(), what, caseSensitive, wholeWord);
	}

	@Override
	protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Set<String> ids, Parameters parameters)
	{
	}

	@Override
	protected void docItSelf(Context context, ReportBuilder report)
	{
        ReportTable table;
        table = report.addTable("", null, true, 100,
                new int[] { 30, 70 }, new String[] { "Chapter", "Description"});

        table.addValues("Destination", "To set event handler for testcases or steps");
        table.addValues("Examples", "<code>#SetHandler</code>");
        table.addValues("See also", "SubCase, TestCase, Step");
	}
	

	@Override
	protected ReturnAndResult executeItSelf(long start, Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		try
		{
		    SubCase subCase = context.referenceToSubcase(this.name.get(), this);
		    HandlerKind handlerKind = HandlerKind.valueOf(this.kind.get());
		    context.setHandler(handlerKind, subCase);

			return new ReturnAndResult(start, Result.Passed, null);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			return new ReturnAndResult(start, Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
		}
	}	

	//==============================================================================================
	// Private members
	//==============================================================================================
	private MutableValue<String> name;
    private MutableValue<String> kind;
}