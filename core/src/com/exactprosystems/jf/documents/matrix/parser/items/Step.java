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
import com.exactprosystems.jf.documents.matrix.parser.Parameter;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.Result;
import com.exactprosystems.jf.documents.matrix.parser.ReturnAndResult;
import com.exactprosystems.jf.documents.matrix.parser.SearchHelper;
import com.exactprosystems.jf.documents.matrix.parser.Tokens;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;
import com.exactprosystems.jf.functions.RowTable;
import com.exactprosystems.jf.functions.Table;

import java.util.List;
import java.util.Map;
import java.util.Set;

@MatrixItemAttribute(
		description 	= "Elementary step in the script", 
		shouldContain 	= { Tokens.Step },
		mayContain 		= { Tokens.Off },
		real			= true,
		hasValue 		= true, 
		hasParameters 	= false,
        hasChildren 	= true
	)
public class Step extends MatrixItem
{
	public Step()
	{
		super();
		this.identify = new Parameter(Tokens.Step.get(),	null); 
	}

	@Override
	public String toString()
	{
		return super.toString() + " " + this.identify.get();
	}

	@Override
	public MatrixItem clone() throws CloneNotSupportedException
	{
		Step clone = (Step) super.clone();
		clone.identify = identify;
		return clone;
	}

	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters)
	{
		this.identify.setExpression(systemParameters.get(Tokens.Step));
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, secondLine, Tokens.Step.get(), 	this.identify.getExpression());
	}

	@Override
	protected void writeSuffixItSelf(CsvWriter writer, List<String> line, String indent)
	{
		super.addParameter(line, Tokens.EndStep.get());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(Tokens.Step.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(this.identify.getExpression(), what, caseSensitive, wholeWord);
	}

	//==============================================================================================
	// Interface Mutable
	//==============================================================================================
    @Override
    public boolean isChanged()
    {
    	if (this.identify.isChanged())
    	{
    		return true;
    	}
    	return super.isChanged();
    }

    @Override
    public void saved()
    {
    	super.saved();
    	this.identify.saved();
    }

	//==============================================================================================
	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.Step.get(), context.getFactory().getSettings());
		driver.showExpressionField(this, layout, 1, 1, Tokens.Step.get(), this.identify, this.identify, null, null, null, null);

		return layout;
	}

	public String getIdentify()
	{
		return this.identify.get();
	}

    @Override
	public String getItemName()
	{
		return super.getItemName() + " " + this.identify.getExpression();
	}

    @Override
	protected void docItSelf(Context context, ReportBuilder report)
	{
        ReportTable table;
        table = report.addTable("", null, true, 100,
                new int[] { 30, 70 }, new String[] { "Chapter", "Description"});

        table.addValues("Destination", "To organize a loop with precondition");
        table.addValues("Examples", "<code>#Step<p>1 + ' small step'</code>");
        table.addValues("See also", "TestCase");
	}
    
    @Override
    protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Set<String> ids, Parameters parameters)
    {
        super.checkItSelf(context, evaluator, listener, ids, parameters);
        this.identify.prepareAndCheck(evaluator, listener, this);
    }
    

    @Override
	protected ReturnAndResult executeItSelf(Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		ReturnAndResult ret = null;
		Result res = null;
		Table table = context.getTable();
		RowTable row = new RowTable();
		int position = -1;

		try
		{
			this.identify.evaluate(evaluator);
			Object identifyValue = this.identify.getValue();
			
			if (table != null)
			{
				position = table.size();
				
				row.put(Context.matrixColumn, 			this.owner.getName());
				TestCase parent = (TestCase)findParent(TestCase.class);
				if (parent != null)
				{
					row.put(Context.testCaseIdColumn, 	parent.getId());
					row.put(Context.testCaseColumn, 	parent);
				}

				row.put(Context.stepIdentityColumn, 	identifyValue);
				row.put(Context.stepColumn, 			this);
				
				table.add(row);
			}
			
			
			
			ret = new ReturnAndResult(Result.Passed);
			res = ret.getResult();
			
			report.outLine(this, null, String.format("Step %s", identifyValue), null);

			ret = executeChildren(context, listener, evaluator, report, new Class<?>[] { OnError.class }, null);
			res = ret.getResult();

			
			if (res == Result.Failed)
			{
				MatrixItem branchOnError = super.find(false, OnError.class, null);
				if (branchOnError != null && branchOnError instanceof OnError)
				{
					((OnError)branchOnError).setError(ret.getError());
					ret = branchOnError.execute(context, listener, evaluator, report);
					res = ret.getResult();
				}
			}

			if (table != null && position >= 0)
			{
				row.put(Context.resultColumn, 			res);
				
				MatrixError error = ret.getError();
				if (error != null)
				{
					row.put(Context.errorKindColumn, 		error.Kind);
					row.put(Context.errorPlaceColumn, 		error.Where);
					row.put(Context.errorPlacePathColumn, 	error.Where.getPath());
					row.put(Context.errorMessageColumn, 	error.Message);
				}
				
				table.updateValue(position, row);
			}
		} 
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			listener.error(this.owner, getNumber(), this, e.getMessage());

			if (table != null && position >= 0)
			{
				row.put(Context.resultColumn, 			res);
				
				row.put(Context.errorKindColumn, 		ErrorKind.EXCEPTION);
				row.put(Context.errorPlaceColumn, 		this);
				row.put(Context.errorPlacePathColumn, 	this.getPath());
				row.put(Context.errorMessageColumn, 	e.getMessage());
				
				table.updateValue(position, row);
			}
			return new ReturnAndResult(Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
		}

		return ret;
	}

	private Parameter identify;
}