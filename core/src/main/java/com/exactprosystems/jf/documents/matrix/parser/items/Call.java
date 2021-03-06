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

package com.exactprosystems.jf.documents.matrix.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.api.error.common.MatrixException;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.evaluator.Variables;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.config.Context.EntryPoint;
import com.exactprosystems.jf.documents.matrix.parser.*;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;
import com.exactprosystems.jf.exceptions.ParametersException;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import com.exactprosystems.jf.tool.matrix.MatrixFx;

import java.util.*;

@MatrixItemAttribute(
		constantGeneralDescription = R.CALL_DESCRIPTION,
		constantExamples = R.CALL_EXAMPLE,
		shouldContain 	= { Tokens.Call },

		mayContain 		= { Tokens.Id, Tokens.Off, Tokens.RepOff },
		parents			= { Case.class, Else.class, For.class, ForEach.class, If.class,
							OnError.class, Step.class, SubCase.class, TestCase.class, While.class },
		real			= true,
		hasValue 		= true, 
		hasParameters 	= true,
        hasChildren 	= false,
		seeAlsoClass 	= {SubCase.class, Return.class}
)
public final class Call extends MatrixItem 
{
	private final MutableValue<String> name;
	private SubCase ref;

	public Call()
	{
		super();
		this.name = new MutableValue<>();
	}

	/**
	 * copy constructor
	 */
	public Call(Call call)
	{
		this.name = new MutableValue<>(call.name);
		if (call.ref != null)
		{
			this.ref = new SubCase(call.ref);
		}
	}

	@Override
	protected MatrixItem makeCopy()
	{
		return new Call(this);
	}

	//region Interface Mutable
	@Override
	public boolean isChanged()
	{
		return this.name.isChanged() || super.isChanged();
	}

	@Override
	public void saved()
	{
		super.saved();
		this.name.saved();
	}
	//endregion

	//region override from MatrixItem
	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 3);
		driver.showComment(this, layout, 0, 0, super.getComments());
		driver.showTextBox(this, layout, 1, 0, this.id, this.id, () -> this.id.get(), null);
		driver.showTitle(this, layout, 1, 1, Tokens.Call.get(), context.getFactory().getSettings());
		driver.showExpressionField(this, layout, 1, 2, Tokens.Call.get(), this.name, this.name,
			str ->
			{
				String res = DialogsHelper.selectFromList(R.COMMON_CHOOSE_SUB_CASE.get(), new ReadableValue(str), context.subcases(this)).getValue();
				if (Str.IsNullOrEmpty(res))
				{
					return res;
				}
				this.updateReference(context, res);
				if (this.ref == null)
				{
					DialogsHelper.showError(String.format(R.CALL_CANT_FIND_SUBCASE.get(), res));
				}
				else
				{
					((MatrixFx) this.getMatrix()).setupCall(this, res, new Parameters(this.ref.getParameters()));
				}
				return res;
			},
			str ->
			{ 
			    EntryPoint entryPoint = context.referenceToSubcase(str, this);
				driver.setCurrentItem(entryPoint.getSubCase(), entryPoint.getMatrix(), false);
				return str;
			}, null, 'G' ); 
		driver.showParameters(this, layout, 1, 3, this.parameters, null, false);
		driver.showCheckBox(this, layout, 2, 0, "Global", this.global, this.global, null);

		return layout;
	}

	@Override
	public Object get(Tokens key)
	{
		if (key == Tokens.Call)
		{
			return this.name.get();
		}
		else
		{
			return super.get(key);
		}
	}
	
	@Override
	public void set(Tokens key, Object value)
	{
		if (key == Tokens.Call)
		{
			this.name.accept((String) value);
		}
		else
		{
			super.set(key, value);
		}
	}

	@Override
	public void addKnownParameters()
	{
		Optional.ofNullable(this.ref)
				.map(SubCase::getParameters)
				.ifPresent(this.parameters::addAll);
	}

	@Override
	public String getItemName()
	{
		return super.getItemName() + " " + this.name;
	}

	@Override
	protected void initItSelf(Map<Tokens, String> systemParameters)
	{
		this.name.accept(systemParameters.get(Tokens.Call));
		this.id.accept(systemParameters.get(Tokens.Id));
	}

	@Override
	protected String itemSuffixSelf()
	{
		return "CALL_";
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, secondLine, TypeMandatory.System, Tokens.Call.get(), this.name.get());
	
		for (Parameter parameter : super.getParameters())
		{
			super.addParameter(firstLine, secondLine, TypeMandatory.Extra, parameter.getName(), parameter.getExpression());
		}
	}

	@Override
	protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
	{
		return SearchHelper.matches(Tokens.Call.get(), what, caseSensitive, wholeWord)
				|| SearchHelper.matches(this.name.get(), what, caseSensitive, wholeWord)
				|| super.getParameters().matches(what,caseSensitive, wholeWord);
	}

	@Override
	protected void checkItSelf(Context context, AbstractEvaluator evaluator, IMatrixListener listener, Parameters parameters)
	{
		super.checkValidId(this.id, listener);
		this.updateReference(context, this.name.get());
		
		if (this.ref == null)
		{
			listener.error(this.owner, super.getNumber(), this, String.format(R.CALL_CANT_FIND_SUBCASE.get(), this.name));
		}
		else
		{
			Set<String> extra = new HashSet<>(parameters.keySet());
			extra.removeAll(this.ref.getParameters().keySet());
			extra.forEach(e -> listener.error(this.owner, super.getNumber(), this, "Extra parameter : " + e));

			Set<String> missed = new HashSet<>(this.ref.getParameters().keySet());
			missed.removeAll(parameters.keySet());
			missed.forEach(m -> listener.error(this.owner, super.getNumber(), this, "Missed parameter : " + m));
		}
	}

	@Override
	protected ReturnAndResult executeItSelf(long start, Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
	{
        Variables locals = evaluator.createLocals(); 
		try
		{
			boolean parametersAreCorrect = parameters.evaluateAll(evaluator);

			if (!parametersAreCorrect)
			{
				this.reportParameters(report, parameters);
				throw new ParametersException(R.CALL_PARAMS_EXCEPTION.get(), parameters);
			}
			if (this.ref == null)
			{
				this.updateReference(context, this.name.get());
			}
			if (this.ref != null)
			{
				evaluator.getLocals().clear();

				this.ref.setRealParameters(parameters);
				boolean isSubcaseIntoMatrix = super.getSource().getRoot().find(true, SubCase.class, this.ref.getId()) == this.ref;
				if (isSubcaseIntoMatrix)
				{
					super.changeState(super.isBreakPoint() ? MatrixItemState.BreakPoint : MatrixItemState.ExecutingParent);
				}
				ReturnAndResult ret = this.ref.execute(context, listener, evaluator, report);
				if (isSubcaseIntoMatrix)
				{
					super.changeState(super.isBreakPoint() ? MatrixItemState.ExecutingWithBreakPoint : MatrixItemState.Executing);
				}

				Result result = ret.getResult();
				if (!Str.IsNullOrEmpty(super.getId()))
				{
					Variables vars = super.isGlobal() ? evaluator.getGlobals() : locals;
					vars.set(super.getId(), ret.getOut());
				}

				if (result.isFail())
				{
					return new ReturnAndResult(start, ret);
				}

				return new ReturnAndResult(start, Result.Passed, ret.getOut());
			}
			report.outLine(this, null, String.format("Sub case '%s' is not found.", this.name), null);
			throw new MatrixException(super.getNumber(), this, String.format(R.CALL_CANT_FIND_SUBCASE.get(), this.name));
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			return new ReturnAndResult(start, Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
		}
		finally
		{
		    evaluator.setLocals(locals);
		}
	}
	//endregion

	void updateReference(Context context, String name)
	{
		this.ref = context.referenceToSubcase(name, this).getSubCase();
	}

	private void reportParameters(ReportBuilder report, Parameters parameters)
	{
		if (!parameters.isEmpty())
		{
			ReportTable table = report.addTable("Input parameters", null, true, true, new int[]{20, 40, 40}, "Parameter", "Expression", "Value");
			parameters.forEach(param -> table.addValues(param.getName(), param.getExpression(), param.getValue()));
		}
	}
}
