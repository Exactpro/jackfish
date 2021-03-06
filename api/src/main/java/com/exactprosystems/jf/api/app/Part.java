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

package com.exactprosystems.jf.api.app;

import com.exactprosystems.jf.api.app.IWindow.SectionKind;
import com.exactprosystems.jf.api.client.ICondition;
import com.exactprosystems.jf.api.common.i18n.R;

import java.io.Serializable;
import java.util.List;

public class Part implements Serializable
{
    private static final long serialVersionUID = -3414311737172884970L;

    public Part(OperationKind kind)
	{
		this.kind = kind;

		this.locatorId = null;
		this.operation = null;
		this.valueCondition = null;
		this.colorCondition = null;
		this.i = -1;
		this.d = Double.NaN;
		this.b = false;
		this.toAppear = false;
		this.x = Integer.MIN_VALUE;
		this.y = Integer.MIN_VALUE;
		this.x2 = Integer.MIN_VALUE;
		this.y2 = Integer.MIN_VALUE;
		this.str = null;
		this.text = null;
		this.key = null;
		this.list = null;
		this.mouse = null;

		this.locatorKind = null;
		this.locator = null;
	}

	public void tune(IWindow window, ITemplateEvaluator templateEvaluator) throws Exception
	{
		if (this.kind == OperationKind.REPEAT && this.operation != null)
		{
			this.operation.tune(window, templateEvaluator);
		}

		if (this.locatorId != null && !this.locatorId.isEmpty() && this.locatorKind != null)
		{
			IControl control = window.getControlForName(SectionKind.Run, this.locatorId);
			if (control == null)
			{
				throw new Exception(String.format(R.PART_TUNE_EXCEPTION.get(), window, this.locatorId));
			}
			//b is true, when we use Do.use(locatorId, locatorKind) and we needn't find another locator
			if (!b)
			{
				switch (this.locatorKind)
				{
					case Element:
						this.locator = control.locator();
						break;

					case Owner:
					case DroppedOwner:
						control = window.getOwnerControl(control);
						break;

					case Header:
						control = window.getHeaderControl(control);
						break;

					case Rows:
						control = window.getRowsControl(control);
						break;
				}
			}

			this.locator = IControl.evaluateTemplate(control, templateEvaluator);
		}

		if (this.locator != null) {
			this.locator = IControl.evaluateTemplate(this.locator, templateEvaluator);
		}
	}

	@Override
	public String toString()
	{
		return this.kind.toFormula(this);
	}

	public Part setInt(int i)
	{
		this.i = i;
		return this;
	}

	public Part setX(int x)
	{
		this.x = x;
		return this;
	}

	public Part setY(int y)
	{
		this.y = y;
		return this;
	}

	public Part setX2(int x2) {
		this.x2 = x2;
		return this;
	}

	public Part setY2(int y2) {
		this.y2 = y2;
		return this;
	}

	public Part setStr(String str)
	{
		this.str = str;
		return this;
	}

	public Part setText(String text)
	{
		this.text = text;
		return this;
	}

	public Part setBool(boolean bool)
	{
		this.b = bool;
		return this;
	}

	public Part setToAppear(boolean toAppear) {
		this.toAppear = toAppear;
		return this;
	}

	public Part setLocatorId(String locator)
	{
		this.locatorId = locator;
		return this;
	}

	public Part setLocator(Locator locator)
	{
		this.locator = locator;
		return this;
	}

	public Part setLocatorKind(LocatorKind locatorKind)
	{
		this.locatorKind = locatorKind;
		return this;
	}

	public Part setValueCondition(ICondition valueCondition)
	{
		this.valueCondition = valueCondition;
		return this;
	}

	public Part setColorCondition(ICondition colorCondition)
	{
		this.colorCondition = colorCondition;
		return this;
	}

	public Part setMouseAction(MouseAction mouse)
	{
		this.mouse = mouse;
		return this;
	}

	public Part setKey(Keyboard key)
	{
		this.key = key;
		return this;
	}
	
	public Part setList(List<String> list)
	{
	    this.list = list;
	    return this;
	}
	

	public Part setValue(double d)
	{
		this.d = d;
		return this;
	}

	public Part setOperation(Operation operation)
	{
		this.operation = operation;
		return this;
	}

	protected OperationKind kind;

	protected Operation operation;
	protected int i;
	protected int x;
	protected int y;
	protected int x2;
	protected int y2;
	protected double d;
	protected boolean b;
	protected boolean toAppear;
	protected String str;
	protected String text;
	protected ICondition valueCondition;
	protected ICondition colorCondition;
	protected MouseAction mouse;
	protected Keyboard key;
	protected List<String> list;

	protected String locatorId;
	protected LocatorKind locatorKind;
	protected Locator locator;
}