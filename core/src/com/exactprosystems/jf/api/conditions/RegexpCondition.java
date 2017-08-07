////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.conditions;

import com.exactprosystems.jf.api.common.DescriptionAttribute;
import com.exactprosystems.jf.api.common.Str;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

@DescriptionAttribute(text = "Returns rows that satisfy the given regular expression")
public class RegexpCondition extends Condition implements Serializable
{
	private static final long serialVersionUID = -1292265002640952551L;

	public RegexpCondition(String name, String pattern)
	{
		super(name);
		this.pattern = pattern;
	}

	@Override
	public String serialize()
	{
		return super.getSerializePrefix(this.getClass()) + start + getName() + separator + this.pattern + finish;
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
		String strValue = "" + value;
		return Pattern.compile(this.pattern).matcher(strValue).find();
	}

	@Override
	public String explanation(String name, Object actualValue)
	{
		return "'" + String.valueOf(actualValue) + "' not suitable for regular expression '" + String.valueOf(this.pattern) + "'";
	}

	@Override
	public String toString()
	{
		return RegexpCondition.class.getSimpleName() + "[name="+getName()+", pattern=" +this.pattern+"]";
	}

	private String pattern;
}