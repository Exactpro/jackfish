////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser;

import com.exactprosystems.jf.api.app.Mutable;

import java.util.Objects;
import java.util.function.BiConsumer;

public class MutableValue<T> implements Mutable, Getter<T>, Setter<T>, Cloneable
{
    private T value = null;
    private boolean changed = false; 
    private BiConsumer<T, T> changeListener = null;

    public MutableValue()
	{
		this.changed = false;
	}

	public MutableValue(T value)
	{
		this();
		this.value = value;
	}

	/**
	 * copy constructor
	 */
	public MutableValue(MutableValue<T> mutableValue)
	{
		this();
		this.value = mutableValue.value;
		this.changed = mutableValue.changed;
	}
	
	@Override
	public int hashCode()
	{
	    return Objects.hashCode(this.value);
	}
	
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof MutableValue))
        {
            return false;
        }
        MutableValue<?> other = (MutableValue<?>) obj;
        return Objects.equals(this.value, other.value);
    }
	
	
	@Override
	public void set(T value)
	{
		this.changed = this.changed || !Objects.equals(this.value, value);
		if (this.changeListener != null)
		{
		    this.changeListener.accept(this.value, value);
		}
		this.value = value;
	}
	
	@Override
	public T get()
	{
		return this.value;
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
	
	public void fire()
	{
	    if (this.changeListener != null)
	    {
	        this.changeListener.accept(this.value, this.value);
	    }
	}

	public void setOnChangeListener(BiConsumer<T, T> listener)
	{
	    this.changeListener = listener;
	}
	
	public boolean isNullOrEmpty()
	{
		return this.value == null || ("" + this.value).isEmpty(); 
	}
}
