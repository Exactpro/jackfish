////////////////////////////////////////////////////////////////////////////////
//Copyright (c) 2009-2015, Exactpro Systems, LLC
//Quality Assurance & Related Development for Innovative Trading Systems.
//All rights reserved.
//This is unpublished, licensed software, confidential and proprietary
//information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.error.common;

import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.api.error.JFException;

public class UnknownChartKindException extends JFException
{
	private static final long serialVersionUID = -7839454497348244047L;

	public UnknownChartKindException(String message)
	{
		super(message, null);
	}

	@Override
	public ErrorKind getErrorKind()
	{
		return ErrorKind.WRONG_PARAMETERS;
	}
}