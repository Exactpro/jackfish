////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser;

import com.exactprosystems.jf.documents.matrix.parser.items.ErrorKind;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixError;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;

public class ReturnAndResult implements Cloneable
{
	public ReturnAndResult(MatrixError error, Result result)
	{
		this.result = result;
		this.out = null;
		this.error = error;
	}

	public ReturnAndResult(Result result, String message, ErrorKind kind, MatrixItem place)
	{
		this.result = result;
		this.out = null;
		this.error = message == null ? null : new MatrixError(message, kind, place);
	}

	public ReturnAndResult(Result result, Object out)
	{
		this.result = result;
		this.out = out;
	}
	
	public ReturnAndResult (Result result)
	{
		this(result, null);
	}
	
	public Result getResult()
	{
		return this.result;
	}

	public Object getOut()
	{
		return this.out;
	}

	public MatrixError getError()
	{
		return this.error;
	}

	private Result result;
	private Object out;
	private MatrixError error;
}
