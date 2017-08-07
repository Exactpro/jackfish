////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.app;

import java.io.Serializable;

import com.exactprosystems.jf.api.common.DescriptionAttribute;

public enum Visibility implements Serializable
{
    @DescriptionAttribute(text = "Make this element visible")
	Visible,
	@DescriptionAttribute(text = "Make this element enabled")
    Enable;
    

	private static final long serialVersionUID = 3590062511245720428L;
}