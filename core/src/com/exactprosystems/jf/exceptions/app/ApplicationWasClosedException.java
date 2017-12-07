////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.exceptions.app;

import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.api.error.JFException;

public class ApplicationWasClosedException extends JFException
{
	public ApplicationWasClosedException(String appId)
	{
		super(String.format("App with id '%s' was closed before", appId));
	}

	@Override
	public ErrorKind getErrorKind()
	{
		return ErrorKind.APPLICATION_CLOSED;
	}
}