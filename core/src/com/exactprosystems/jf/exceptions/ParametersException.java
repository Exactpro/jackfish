package com.exactprosystems.jf.exceptions;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.jf.documents.matrix.parser.Parameter;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

public class ParametersException extends Exception
{
	private static final long	serialVersionUID	= 2381152373172778652L;

	public ParametersException(String message, Parameters params)
	{
		this(message, null, params);
	}

	public ParametersException(String message, Throwable cause, Parameters params)
	{
		super(message, cause);
		this.params = params;
	}

	public List<String> getParameterErrors()
	{
		List<String> res = new ArrayList<String>();
		for (Parameter param : this.params)
		{
			if (!param.isValid())
			{
				res.add(param.getName() + ":" + param.getValueAsString());
			}
		}
		return res;
	}

	private Parameters params;
}
