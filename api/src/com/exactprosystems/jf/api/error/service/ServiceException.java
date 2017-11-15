////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.error.service;

import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.api.error.JFException;

public class ServiceException extends JFException
{
	private static final long	serialVersionUID	= 2653699261504393579L;

	public ServiceException(String message)
	{
		super(message, null);
	}

	@Override
	public ErrorKind getErrorKind()
	{
		return ErrorKind.SERVICE_ERROR;
	}
}
