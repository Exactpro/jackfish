////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.DisplayDriver;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.Parser;
import com.exactprosystems.jf.documents.matrix.parser.Result;
import com.exactprosystems.jf.documents.matrix.parser.ReturnAndResult;
import com.exactprosystems.jf.documents.matrix.parser.SearchHelper;
import com.exactprosystems.jf.documents.matrix.parser.Tokens;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;

import java.util.List;

@MatrixItemAttribute(
		description 	= "On error.",
		examples 		= "#OnError",
		seeAlso 		= "Fail",
		shouldContain 	= { Tokens.OnError },
		mayContain 		= { Tokens.Off, Tokens.RepOff },
		parents			= { For.class, ForEach.class, OnError.class, Step.class, SubCase.class, TestCase.class },
		real			= true,
		hasValue 		= false, 
		hasParameters 	= false,
        hasChildren 	= true
	)
public final class OnError extends MatrixItem 
{	
	public OnError()
	{
		super();
	}

	public void setError(MatrixError error)
	{
		this.matrixError = error;
	}

	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.OnError.get(), context.getFactory().getSettings());

		return layout;
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, Tokens.OnError.get());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(Tokens.OnError.get(), what, caseSensitive, wholeWord);
	}

	@Override
	protected ReturnAndResult executeItSelf(long start, Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		try
		{
			evaluator.getLocals().getVars().put(Parser.error, 	this.matrixError == null ? null : this.matrixError.Message);
			evaluator.getLocals().getVars().put(Parser.err, 	this.matrixError == null ? new MatrixError("Unknown", ErrorKind.OTHER, this) : this.matrixError);
			ReturnAndResult ret = super.executeItSelf(start, context, listener, evaluator, report, parameters);
			Result result = ret.getResult();
					
			if (result == Result.Failed)
			{
				MatrixItem branchOnError = super.find(false, OnError.class, null);
				if (branchOnError != null && branchOnError instanceof OnError)
				{
					((OnError)branchOnError).setError(ret.getError());
					
					ret = branchOnError.execute(context, listener, evaluator, report);
				}
			}

			return ret;
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			listener.error(this.owner, getNumber(), this, e.getMessage());
			return new ReturnAndResult(start, Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
		}
	}
	
	private MatrixError matrixError = null;
}
