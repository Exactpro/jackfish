////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.common.xml.control;

import com.exactprosystems.jf.api.app.ControlKind;
import com.exactprosystems.jf.api.app.Part;
import com.exactprosystems.jf.common.ControlsAttributes;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@ControlsAttributes(
		bindedClass 		= ControlKind.TabPanel
)
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class TabPanel extends AbstractControl
{
	public TabPanel()
	{
	}

	@Override
	public void prepare(Part part, Object value) throws Exception
	{
		if (value instanceof Number)
		{
			part.setValue(((Number)value).longValue());
		}
	}
}
