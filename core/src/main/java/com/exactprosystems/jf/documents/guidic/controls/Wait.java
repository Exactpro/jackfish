/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.jf.documents.guidic.controls;

import com.exactprosystems.jf.api.app.Addition;
import com.exactprosystems.jf.api.app.ControlKind;
import com.exactprosystems.jf.api.app.Part;
import com.exactprosystems.jf.common.ControlsAttributes;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@ControlsAttributes(
		bindedClass 		= ControlKind.Wait
)
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Wait extends AbstractControl
{
	public Wait()
	{
	}
	
	@Override
	public void prepare(Part part, Object value) throws Exception
	{
		if (value == null)
		{
			part.setLocator(locator());
			part.setInt(getTimeout());
			part.setToAppear(this.addition == Addition.WaitToAppear);
		}
		if (value instanceof String)
		{
			part.setText(String.valueOf(value));
		}
		else if (value instanceof Number)
		{
			part.setInt(((Number)value).intValue());
		}
	}
}