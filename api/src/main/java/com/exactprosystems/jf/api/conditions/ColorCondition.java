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

package com.exactprosystems.jf.api.conditions;

import com.exactprosystems.jf.api.app.InnerColor;
import com.exactprosystems.jf.api.common.DescriptionAttribute;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;

import java.awt.*;
import java.io.Serializable;
import java.util.Map;

@DescriptionAttribute(text = R.COLOR_CONDITION_DESCRIPTION)
public class ColorCondition extends Condition implements Serializable
{

	private static final long serialVersionUID = -2853180365570996848L;

	public ColorCondition(String name, Color value, boolean foreground)
	{
		super(name);
		this.value = value == null ? null : new InnerColor(value);
		this.foreground = foreground;
	}

	public ColorCondition(String name, Color value)
	{
		this(name, value, true);
	}

	@Override
	public String serialize()
	{
		return super.getSerializePrefix(this.getClass()) + start + getName() + separator + this.value + separator + this.foreground + finish;
	}
	
	@Override
	public String toString()
	{
		return ColorCondition.class.getSimpleName() + " [name=" + getName() + ", value=" + value  + ", foreground=" + foreground + "]";
	}

	@Override
	public boolean isMatched(Map<String, Object> map)
	{
		String name = getName();
		if (Str.IsNullOrEmpty(name))
		{
			return true;
		}
		Object value = map.get(name);

		return value instanceof Color && this.value.equals(value);
	}

	@Override
	public String explanation(String name, Object actualValue)
	{
		return String.valueOf(this.value) + " != " + actualValue;
	}

	public Color getColor()
	{
	    if (this.value != null)
	    {
	        return this.value;
	    }
	    return Color.black;
	}

	public String colorToString()
	{

		/*
			TODO be careful, when change type from InnerColor to Color.
			We need ovveride convert our color to value 'Color[1, 2, 3]'
			or change pattern on win side
			Now, pattern to parse color of win side : "Color\\[(\\d+),\\s?(\\d+),\\s?(\\d+)\\]"
		 */
		return this.value.toString();
	}
	
	private InnerColor value = null;

	private boolean foreground = true;
}
