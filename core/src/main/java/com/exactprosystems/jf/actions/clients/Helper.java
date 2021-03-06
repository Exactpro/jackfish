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

package com.exactprosystems.jf.actions.clients;

import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.client.*;
import com.exactprosystems.jf.api.common.ParametersKind;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

import java.util.List;

public class Helper
{
	private Helper()
	{}

	public static void clientsNames(List<ReadableValue> list, Context context)
	{
		context.getConfiguration().getClientPool().clientNames()
				.stream()
				.map(context.getEvaluator()::createString)
				.map(ReadableValue::new)
				.forEach(list::add);
	}
	
	public static IClientFactory getFactory(Matrix matrix, Context context, Parameters parameters, String clientName, String connectionName) throws Exception
	{
		try
		{
			parameters.evaluateAll(context.getEvaluator());
		}
		catch (Exception e)
		{
			// nothing to do
		}

		if (clientName != null)
		{
			try
			{
		 		Object client = parameters.get(clientName);
				if (client != null)
				{
					return context.getConfiguration().getClientPool().loadClientFactory(client.toString());
				}
			}
			catch (Exception e)
			{
				// nothing to do
			}
		}
		
		if (connectionName != null)
		{
			try
			{
		 		Object connection = parameters.get(connectionName);
				if (connection instanceof ClientConnection)
				{
					return ((ClientConnection)connection).getClient().getFactory();
				}
			}
			catch (Exception e)
			{
				// nothing to do
			}
		
		}
		
		IClientFactory factory = matrix.getDefaultClient();
		if (factory == null)
		{
			throw new Exception(R.CLIENTS_HELPER_CHOOSE_CLIENT_ERROR.get());
		}
		
		return factory;
	}
	
	public static void messageTypes(List<ReadableValue> list, Matrix matrix, Context context, Parameters parameters, String clientName, String connectionName) throws Exception
	{
		IClientFactory factory = getFactory(matrix, context, parameters, clientName, connectionName);
		AbstractEvaluator evaluator = context.getEvaluator();
		IMessageDictionary dic = factory.getDictionary();
		dic.getMessages().stream()
				.map(message -> new ReadableValue(evaluator.createString(message.getName())))
				.forEach(list::add);
	}

	public static void messageValues(List<ReadableValue> list, Context context, Matrix matrix, Parameters parameters, String clientName, String connectionName, String messageTypeName, String fieldName) throws Exception
	{
		IClientFactory factory = getFactory(matrix, context, parameters, clientName, connectionName);
		AbstractEvaluator evaluator = context.getEvaluator();
		IMessageDictionary dic = factory.getDictionary();
		String messageType = Str.asString(parameters.get(messageTypeName));
		IMessage message = dic.getMessage(messageType);
		if (message == null)
		{
			throw new Exception(String.format(R.CLIENTS_HELPER_UNKNOWN_MESSAGE_TYPE.get(), messageType));
		}
		IField field = message.getDeepField(fieldName);
		if (field == null)
		{
			throw new Exception(String.format(R.CLIENTS_HELPER_UNKNOWN_FIELD_NAME.get(), fieldName));
		}
		if (field.getReference() != null)
		{
			field = field.getReference();
		}
		Class<?> type = field.getType().getJavaClass();
		boolean needQuotes = type != null && (type == String.class || type == Character.class);
		if (type == Boolean.class)
		{
			list.add(ReadableValue.TRUE);
			list.add(ReadableValue.FALSE);
		}
		for(IAttribute attr : field.getValues())
		{
			String str = attr.getValue();
			if (needQuotes)
			{
				str = evaluator.createString(str);
			}

			if (!Str.IsNullOrEmpty(str))
			{
				list.add(new ReadableValue(str, attr.getName()));
			}
		}
	}

	public static void helpToAddParameters(List<ReadableValue> list, ParametersKind kind, Context context, Matrix matrix, Parameters parameters, String clientName, String connectionName, String messageTypeName) throws Exception
	{
		IClientFactory factory = getFactory(matrix, context, parameters, clientName, connectionName);
		IMessageDictionary dic = factory.getDictionary();
		
		for (String arg : factory.wellKnownParameters(kind))
		{
			list.add(new ReadableValue(arg));
		}
		
		if (messageTypeName != null)
		{
			String messageType = Str.asString(parameters.get(messageTypeName));
			if (!Str.areEqual(messageType, "*"))
			{
				IMessage message = dic.getMessage(messageType);
				if (message == null)
				{
					throw new Exception(String.format(R.CLIENTS_HELPER_UNKNOWN_MESSAGE_TYPE.get(), messageType));
				}
				for (IField field : message.getFields())
				{
					list.add(new ReadableValue(field.getName()));
				}
			}
		}
	}
	
	public static boolean canFillParameter(Matrix matrix, Context context, Parameters parameters, String idName, String connectionName, String parameterName) throws Exception
	{
		IClientFactory factory = getFactory(matrix, context, parameters, idName, connectionName);
		IMessageDictionary dic = factory.getDictionary();
		IField field = dic.getField(parameterName);
		if (field == null)
		{
			IMessage messageByType = dic.getMessage(parameters.getByName("MessageType").getValueAsString());
			if (messageByType == null)
			{
				return false;
			}
			return checkField(messageByType.getField(parameterName));
		}

		return checkField(field);
	}

	private static boolean checkField(IField field)
	{
		if (field == null)
		{
			return false;
		}
		IField reference = field.getReference();
		if (reference != null)
		{
			return reference.getType().getJavaClass() == Boolean.class || reference.getValues() != null && !reference.getValues().isEmpty();
		}
		return  field.getType().getJavaClass() == Boolean.class || field.getValues() != null && !field.getValues().isEmpty();
	}

}
