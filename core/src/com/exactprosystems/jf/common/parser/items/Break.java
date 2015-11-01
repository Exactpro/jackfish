////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.common.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.common.Context;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.parser.*;
import com.exactprosystems.jf.common.parser.listeners.IMatrixListener;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;

import java.util.List;

@MatrixItemAttribute(
		description 	= "Break chain of execution into loops or blocks.", 
		shouldContain 	= { Tokens.Break },
		mayContain 		= { Tokens.Off },
		real			= true,
		hasValue 		= false, 
		hasParameters 	= false,
        hasChildren 	= false
	)
public class Break extends MatrixItem
{
    public Break()
    {
        super();
    }

	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.Break.get());

		return layout;
	}

    @Override
	protected void docItSelf(Context context, ReportBuilder report)
	{
        ReportTable table;
        table = report.addTable("", 100, new int[] { 30, 70 },
                new String[] { "Chapter", "Description"});

        table.addValues("Destination", "To break loops for and while");
        table.addValues("Examples", "<code>#Break</code>");
        table.addValues("See also", "For, While, Continue");
	}
	

	@Override
	protected ReturnAndResult executeItSelf(Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		return new ReturnAndResult(Result.Break); 
	}

    @Override
    protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
    {
        super.addParameter(firstLine, Tokens.Break.get());
    }

    @Override
    protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
    {
        return SearchHelper.matches(Tokens.Break.get(), what, caseSensitive, wholeWord);
    }
}
