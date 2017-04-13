////////////////////////////////////////////////////////////////////////////////
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.functions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class RowTable implements Map<String, Object>, Cloneable
{
	public RowTable(Map<Header, Object> map)
	{
	    this();
	    
		if (map == null)
		{
			throw new NullPointerException("map");
		}
		map.forEach((k,v) -> this.source.put(k.name, v));
	}

	public RowTable()
	{
	    this.source = new LinkedHashMap<String, Object>();
	}
	
    public void keepOnly(Set<String> names)
    {
        this.source = this.source.entrySet()
                .stream()
                .filter(e -> names.contains(e.getKey()))
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()));
    }

    @Override
	public String toString()
	{
		return this.source.toString();
	}
	
	@Override
    public int hashCode()
    {
        return Objects.hashCode(this.source);
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
        if (getClass() != obj.getClass())
        {
            return false;
        }
        RowTable other = (RowTable) obj;
        return Objects.equals(this.source, other.source);
    }

    //==============================================================================================
	// Interface Cloneable
	//==============================================================================================
	@Override
	public RowTable clone() throws CloneNotSupportedException
	{
		RowTable clone = new RowTable(); 
		clone.putAll(this);
		return clone;
	}


	//==============================================================================================
	// Interface Map
	//==============================================================================================

	@Override
	public int size()
	{
		return this.source.size();
	}

	@Override
	public boolean isEmpty()
	{
		return this.source.isEmpty();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return this.source.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return this.source.containsValue(value);
	}

	@Override
	public Object get(Object key)
	{
	    return this.source.get(key);
	}

	@Override
	public Object put(String key, Object value)
	{
	    return this.source.put(key, value);
	}

	@Override
	public Object remove(Object key)
	{
	    return this.source.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m)
	{
		this.source.putAll(m);
	}

	@Override
	public void clear()
	{
	    this.source.clear();
	}

	@Override
	public Set<String> keySet()
	{
	    return this.source.keySet();
	}

	@Override
	public Collection<Object> values()
	{
		return this.source.values();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet()
	{
	    return this.source.entrySet();
	}

    private Map<String, Object> source;
}
