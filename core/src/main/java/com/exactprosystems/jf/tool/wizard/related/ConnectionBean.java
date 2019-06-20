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

package com.exactprosystems.jf.tool.wizard.related;

import com.exactprosystems.jf.api.app.AppConnection;

public class ConnectionBean
{
	private String        name;
	private AppConnection connection;

	public ConnectionBean(String name, AppConnection connection)
	{
		this.name = name;
		this.connection = connection;
	}

	public String getName()
	{
		return name;
	}

	public AppConnection getConnection()
	{
		return connection;
	}

	@Override
	public String toString()
	{
		return this.name + " [ " + this.connection.toString() + " ]";
	}

}