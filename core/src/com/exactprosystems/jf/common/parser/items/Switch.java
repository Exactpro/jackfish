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
        description 	= "Condition executing.",
        shouldContain 	= { Tokens.Switch },
        mayContain 		= { Tokens.Off },
        real			= true,
        hasValue 		= true,
        hasParameters 	= false,
        hasChildren 	= true
)
public class Switch extends MatrixItem
{
	public Switch()
	{
		super();
		this.switcher	= new Parameter(Tokens.Switch.get(),	 null); 
	}

	@Override
	public MatrixItem clone() throws CloneNotSupportedException
	{
		Switch clone = (Switch) super.clone();
		clone.switcher = switcher.clone();
		return clone;
	}

	//==============================================================================================
	// Interface Mutable
	//==============================================================================================
    @Override
    public boolean isChanged()
    {
    	if (this.switcher.isChanged())
    	{
    		return true;
    	}
    	return super.isChanged();
    }

    @Override
    public void saved()
    {
    	super.saved();
    	this.switcher.saved();
    }

	//==============================================================================================
	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.Switch.get());
		driver.showExpressionField(this, layout, 1, 1, Tokens.Switch.get(), this.switcher, this.switcher, null, null, null, null);

		return layout;
	}

	@Override
	public String getItemName()
	{
		return super.getItemName() + " " + this.switcher;
	}

	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters) throws MatrixException
	{
		this.switcher.setExpression(systemParameters.get(Tokens.Switch));
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, secondLine, Tokens.Switch.get(), this.switcher.getExpression());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(Tokens.Switch.get(), what, caseSensitive, wholeWord) ||
				SearchHelper.matches(this.switcher.getExpression(), what, caseSensitive, wholeWord);
	}

	@Override
	protected void writeSuffixItSelf(CsvWriter writer, List<String> line, String indent)
	{
		super.addParameter(line, Tokens.EndSwitch.get());
	}

	@Override
	protected void docItSelf(Context context, ReportBuilder report)
	{
        ReportTable table;
        table = report.addTable("", 100, new int[] { 30, 70 },
                new String[] { "Chapter", "Description"});

        table.addValues("Destination", "To check a condition and execute one or another branch of script");
        table.addValues("Examples", "<code>#Switch</code>");
        table.addValues("See also", "Case,Default");
	}
	
    @Override
    protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Set<String> ids, Parameters parameters)
    {
        super.checkItSelf(context, evaluator, listener, ids, parameters);
        this.switcher.prepareAndCheck(evaluator, listener, this);
        
        for (MatrixItem child : this.children)
        {
        	if (!(child instanceof Case || child instanceof Default))
        	{
        		listener.error(getMatrix(), getNumber(), this, "Switch must contain only Case or Default item. But contains " + child.getItemName());
        	}
        	else
        	{
        		child.checkItSelf(context, evaluator, listener, ids, parameters);
        	}
        }
    }
    

	@Override
	protected ReturnAndResult executeItSelf(Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		try
		{
			ReturnAndResult ret = new ReturnAndResult(Result.Passed);
			
			this.switcher.evaluate(evaluator);
			if (!this.switcher.isValid())
			{
				ReportTable table = report.addTable("Switch", 1, new int[] {50, 50}, 
						new String[] {"Expression", "Error"});
			
				String msg = "Error in expression #Switch";
	        	table.addValues(this.switcher.getExpression(), msg);

	        	throw new Exception(msg);
			}
			
			Object eval = this.switcher.getValue();

			for (MatrixItem item : this.children)
			{
				if (item instanceof Case)
				{
					Case caze = (Case)item;
					Parameter variant = caze.getVariant();
					if (variant.evaluate(evaluator))
					{
						Object value = variant.getValue();
						if (value == null)
						{
							if (eval == null)
							{
								ret = item.execute(context, listener, evaluator, report);
								break;
							}
						}
						else
						{
							if (value.equals(eval))
							{
								ret = item.execute(context, listener, evaluator, report);
								break;
							}
						}
					}
					else
					{
						throw new Exception("Error in expression:" + item.getItemName());
					}
				}
				else if (item instanceof Default)
				{
					ret = item.execute(context, listener, evaluator, report);
					break;
				}
			}
			
			return ret;
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			listener.error(this.owner, getNumber(), this, e.getMessage());
			return new ReturnAndResult(Result.Failed, null, e.getMessage());
		}
	}

	private Parameter switcher;
}
