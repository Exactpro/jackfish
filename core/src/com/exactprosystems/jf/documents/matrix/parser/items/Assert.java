////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.api.error.common.MatrixException;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.*;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;

import java.util.List;
import java.util.Map;

@MatrixItemAttribute(
		description 	= "This operator is used to check an assertion. If an assertion is correct, the matrix will run," +
							" if an assertion is incorrect, the operator is terminated and it is reported in user's message." +
							" This operator is used to check equality of expected and actual values." +
							"Fields:\n" +
							"Assert - assertion.\n" +
							"Message - a message, which will be reported",
		examples 		= "{{#\n" +
							"#Assert;#Message\n" + "a == 10;'The value of ' + a + ' doesn match 10'#}}",
		shouldContain 	= { Tokens.Assert },
		mayContain 		= { Tokens.Off, Tokens.RepOff, Tokens.Message }, 
		parents			= { Case.class, Else.class, For.class, ForEach.class, If.class,
							OnError.class, Step.class, SubCase.class, TestCase.class, While.class },
		real			= true,
		hasValue 		= true, 
		hasParameters 	= false,
        hasChildren 	= false
	)
public class Assert extends MatrixItem
{
	private Parameter assertion = null;
	private Parameter message = null;

	public Assert()
	{
		super();
		this.assertion = new Parameter(Tokens.Assert.get(), null);
		this.message = new Parameter(Tokens.Message.get(), null);
	}

	/**
	 * copy constructor
	 */
	public Assert(Assert oldAssert)
	{
		this.assertion = new Parameter(oldAssert.assertion);
		this.message = new Parameter(oldAssert.message);
	}

	public MatrixItem makeCopy()
	{
		return new Assert(this);
	}

	//==============================================================================================
	// Interface Mutable
	//==============================================================================================
	@Override
	public boolean isChanged()
	{
		if (this.assertion.isChanged() || this.message.isChanged())
		{
			return true;
		}
		return super.isChanged();
	}

	@Override
	public void saved()
	{
		super.saved();
		this.assertion.saved();
		this.message.saved();
	}

	//==============================================================================================
	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.Assert.get(), context.getFactory().getSettings());
		driver.showExpressionField(this, layout, 1, 1, Tokens.Assert.get(), this.assertion, this.assertion, null, null, null, null);
		driver.showLabel(this, layout, 1, 2, Tokens.Message.get());
		driver.showExpressionField(this, layout, 1, 3, Tokens.Message.get(), this.message, this.message, null, null, null, null);

		return layout;
	}

	@Override
	public String getItemName()
	{
		return super.getItemName() + " " + (this.assertion.getExpression() == null ? "" : ": " + this.assertion.getExpression()) + " " + (this.message.getExpression() == null ? "" : this.message);
	}

	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters) throws MatrixException
	{
		this.assertion.setExpression(systemParameters.get(Tokens.Assert));
		this.message.setExpression(systemParameters.get(Tokens.Message));
	}


	@Override
	protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Parameters parameters)
	{
		super.checkItSelf(context, evaluator, listener, parameters);
		this.assertion.prepareAndCheck(evaluator, listener, this);
		this.message.prepareAndCheck(evaluator, listener, this);
	}

	@Override
	protected ReturnAndResult executeItSelf(long start, Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
		try
		{
			this.assertion.evaluate(evaluator);
			this.message.evaluate(evaluator);
			if (!this.assertion.isValid() || !this.message.isValid())
			{
				ReportTable table = report.addTable("Assert", null, true, true, new int[]{50, 50}, new String[]{"Expression", "Error"});

				String msg = "Error in expression ";
				if (!this.assertion.isValid())
				{
					msg += " #Assert";
					table.addValues(this.assertion.getExpression(), this.assertion.getValueAsString());
				}
				if (!this.message.isValid())
				{
					msg += " #Message";
					table.addValues(this.message.getExpression(), this.message.getValueAsString());
				}

				return new ReturnAndResult(start, Result.Failed, msg, ErrorKind.EXCEPTION, this);
			}


			Object eval = this.assertion.getValue();
			if (eval instanceof Boolean)
			{
				ReportTable table = report.addTable("Assert", null, true, true, new int[]{50, 50}, new String[]{"Expression", "Value"});

				boolean bool = (Boolean) eval;
				if (bool)
				{
					table.addValues(this.assertion.getExpression(), bool);
					report.itemIntermediate(this);

					return new ReturnAndResult(start, Result.Passed, null);
				}
				else
				{
					table.addValues(this.assertion.getExpression(), bool);
					table.addValues(this.message.getExpression(), this.message.getValueAsString());
					report.itemIntermediate(this);

					return new ReturnAndResult(start, Result.Failed, this.message.getValueAsString(), ErrorKind.ASSERT, this);
				}
			}

			return new ReturnAndResult(start, Result.Failed, "Assert expression should be boolean", ErrorKind.EXPRESSION_ERROR, this);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			listener.error(this.owner, getNumber(), this, e.getMessage());
			return new ReturnAndResult(start, Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
		}
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, secondLine, TypeMandatory.System, Tokens.Assert.get(), this.assertion.getExpression());
		super.addParameter(firstLine, secondLine, TypeMandatory.System, Tokens.Message.get(), this.message.getExpression());
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(Tokens.Assert.get(), what, caseSensitive, wholeWord) || SearchHelper.matches(this.assertion.getExpression(), what, caseSensitive, wholeWord)
				|| SearchHelper.matches(this.message.getExpression(), what, caseSensitive, wholeWord);
	}
}

