////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser;

import com.exactprosystems.jf.api.app.Mutable;

public class MutableValue<T> implements Mutable, Getter<T>, Setter<T>, Cloneable
{
	public MutableValue()
	{
		this.changed = false;
	}

	public MutableValue(T value)
	{
		this();
		this.value = value;
	}
	
	@Override
	public void set(T value)
	{
		this.changed = this.changed || !areEqual(this.value, value);
		this.value = value;
	}
	
	@Override
	public T get()
	{
		return this.value;
	}

	@Override
	public MutableValue<T> clone() throws CloneNotSupportedException
	{
		@SuppressWarnings("unchecked")
		MutableValue<T> clone = ((MutableValue<T>) super.clone());
		clone.value = this.value;
		clone.changed = this.changed;

		return clone;
	}

	@Override
	public boolean isChanged()
	{
		return this.changed;
	}

	@Override
	public void saved()
	{
		this.changed = false;
	}

	@Override
	public String toString()
	{
		return "" + (this.value == null ? "" : this.value.toString());
	}
	
	public boolean isNullOrEmpty()
	{
		return this.value == null || ("" + this.value).isEmpty(); 
	}

	
    private static boolean areEqual(Object s1, Object s2)
    {
    	if (s1 == null)
    	{
    		return s1 == s2;
    	}
    	
    	return s1.equals(s2);
    }

	
	private T value = null;
	
	private boolean changed = false; 
}
