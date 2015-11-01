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
import java.util.Map;
import java.util.Set;

@MatrixItemAttribute(
		description 	= "Loop from start value to end value with step.", 
		shouldContain 	= { Tokens.For, Tokens.From, Tokens.To },
		mayContain 		= { Tokens.Step, Tokens.Off }, 
		real			= true,
		hasValue 		= true, 
		hasParameters 	= false,
        hasChildren 	= true
	)
public final class For extends MatrixItem
{
	public For()
	{
		super();
		this.var	= new MutableValue<String>();
		this.from	= new Parameter(Tokens.From.get(),	null); 
		this.to		= new Parameter(Tokens.To.get(), 		null); 
		this.step	= new Parameter(Tokens.Step.get(), 	null); 
	}

	@Override
	public MatrixItem clone() throws CloneNotSupportedException
	{
		For clone = ((For) super.clone());
		clone.var = var.clone();
		clone.from = from.clone();
		clone.to = to.clone();
		clone.step = step.clone();
		return clone;
	}
	
	//==============================================================================================
	// Interface Mutable
	//==============================================================================================
    @Override
    public boolean isChanged()
    {
    	if (	this.var.isChanged()
    		||	this.from.isChanged()
    		|| 	this.to.isChanged()
    		|| 	this.step.isChanged() )
    	{
    		return true;
    	}
    	return super.isChanged();
    }

    @Override
    public void saved()
    {
    	super.saved();
    	this.var.saved();
    	this.from.saved();
    	this.to.saved();
    	this.step.saved();
    }

	//==============================================================================================
	// implements Displayed
	//==============================================================================================
	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.For.get());
		driver.showTextBox(this, layout, 1, 1, this.var, this.var, () -> this.var.get());
		driver.showLabel(this, layout, 1, 2, Tokens.From.get());
		driver.showExpressionField(this, layout, 1, 3, Tokens.From.get(), this.from, this.from, null, null, null, null);
		driver.showLabel(this, layout, 1, 4, Tokens.To.get());
		driver.showExpressionField(this, layout, 1, 5, Tokens.To.get(), this.to, this.to, null, null, null, null);
		driver.showLabel(this, layout, 1, 6, Tokens.Step.get());
		driver.showExpressionField(this, layout, 1, 7, Tokens.Step.get(), this.step, this.step, null, null, null, null);

		return layout;
	}

	//==============================================================================================

	@Override
	public String getItemName()
	{
		return super.getItemName() + " " + this.var + " = " + this.from + " to " + this.to + " step " + this.step;
	}
	
	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters) 
			throws MatrixException
	{
		super.initItSelf(systemParameters);
		
		this.var.set(systemParameters.get(Tokens.For)); 
		this.from.setExpression(systemParameters.get(Tokens.From)); 
		this.to.setExpression(systemParameters.get(Tokens.To)); 
		this.step.setExpression(systemParameters.get(Tokens.Step)); 
		if (this.step == null)
		{
			this.step.setExpression("1");
		}
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, secondLine, Tokens.For.get(), 	this.var.get());
		super.addParameter(firstLine, secondLine, Tokens.From.get(), 	this.from.getExpression());
		super.addParameter(firstLine, secondLine, Tokens.To.get(), 		this.to.getExpression());
		super.addParameter(firstLine, secondLine, Tokens.Step.get(), 	this.step.getExpression());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(this.var.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(Tokens.For.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(Tokens.From.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(Tokens.Step.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(this.from.getExpression(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(this.to.getExpression(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(this.step.getExpression(), what, caseSensitive, wholeWord);
	}

	@Override
	protected void writeSuffixItSelf(CsvWriter writer, List<String> line, String indent)
	{
		super.addParameter(line, Tokens.EndFor.get());
	}


    @Override
	protected void docItSelf(Context context, ReportBuilder report)
	{
        ReportTable table;
        table = report.addTable("", 100, new int[] { 30, 70 },
                new String[] { "Chapter", "Description"});

        table.addValues("Destination", "To organize a loop for counter from begin value to end value with step");
        table.addValues("Examples", "<code>#For;#From;#To;#Step<p>i;1;100;2</code>");
        table.addValues("See also", "While, Break, Continue");
	}
	
    @Override
    protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Set<String> ids, Parameters parameters)
    {
        super.checkItSelf(context, evaluator, listener, ids, parameters);
        this.from.prepareAndCheck(evaluator, listener, this);
        this.to.prepareAndCheck(evaluator, listener, this);
        this.step.prepareAndCheck(evaluator, listener, this);
    }
    
	@Override
	protected ReturnAndResult executeItSelf(Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		try
		{
			ReturnAndResult ret = new ReturnAndResult(Result.Passed, null);
			Result result = ret.getResult();
			boolean wasError = false;
			
			if (!this.from.evaluate(evaluator))
			{
				throw new Exception("Error in expression #From");
			}
			if (!this.to.evaluate(evaluator))
			{
				throw new Exception("Error in expression #To");
			}
			if (!this.step.evaluate(evaluator))
			{
				throw new Exception("Error in expression #Step");
			}
			
			Object fromValue 	= this.from.getValue();
			Object toValue 		= this.to.getValue();
			Object stepValue 	= this.step.getValue();
			if (!(fromValue instanceof Number))
			{
				throw new Exception("#From is not type of Number");
			}
			if (!(toValue instanceof Number))
			{
				throw new Exception("#To is not type of Number");
			}
			if (!(stepValue instanceof Number))
			{
				throw new Exception("#Step is not type of Number");
			}
			
			// start value
			Number currentValue = (Number)fromValue;
			evaluator.getLocals().set(this.var.get(), currentValue);
			boolean condition = ((Number)currentValue).intValue() <= ((Number)toValue).intValue();

			while(condition)
			{
				report.outLine(this, String.format("loop %s = %s", this.var, currentValue), currentValue.intValue());
				
				ret = executeChildren(context, listener, evaluator, report, new Class<?>[] { OnError.class }, null);
				result = ret.getResult();
				
				currentValue = currentValue.intValue() + ((Number)stepValue).intValue(); 
				condition = ((Number)currentValue).intValue() <= ((Number)toValue).intValue();
				evaluator.getLocals().set(this.var.get(), currentValue);

				if (result == Result.Failed)
				{
					wasError = true;
					
					MatrixItem branchOnError = super.find(false, OnError.class, null);
					if (branchOnError != null && branchOnError instanceof OnError)
					{
						((OnError)branchOnError).setError(ret.getError());
						
						ret = branchOnError.execute(context, listener, evaluator, report);
						result = ret.getResult();
					}
					else
					{
						return ret;
					}
				}

				if (result == Result.Stopped || result == Result.Break || result == Result.Return)
				{
					break;
				}
				if (result == Result.Continue)
				{
					continue;
				}
			}

			if (wasError)
			{
				return new ReturnAndResult(Result.Failed, ret.getOut(), ret.getError());
			}
			return new ReturnAndResult(Result.Passed, ret.getOut()); 
		} 
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			listener.error(this.owner, getNumber(), this, e.getMessage());
			return new ReturnAndResult(Result.Failed, null, e.getMessage());
		}
	}

	private MutableValue<String> var; 
	private Parameter from; 
	private Parameter to; 
	private Parameter step;
}
