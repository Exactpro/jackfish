////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.clients;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.client.ClientConnection;
import com.exactprosystems.jf.api.common.ParametersKind;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.items.TypeMandatory;
import com.exactprosystems.jf.functions.HelpKind;

import java.net.Socket;
import java.util.List;

@ActionAttribute(
		group 				  	      = ActionGroups.Clients,
		suffix 				  	      = "CLCNCT",
		constantGeneralDescription    = R.CLIENT_CONNECT_GENERAL_DESC,
		additionFieldsAllowed 	      = true,
		constantOutputDescription     = R.CLIENT_CONNECT_OUTPUT_DESC,
		constantAdditionalDescription = R.CLIENT_CONNECT_ADDITIONAL_DESC,
		constantExamples 			  = R.CLIENT_CONNECT_EXAMPLE
)
public class ClientConnect extends AbstractAction
{
	public static final String connectionName = "ClientConnection";
	public static final String socketName     = "Socket";

	@ActionFieldAttribute(name = connectionName, mandatory = true, constantDescription = R.CLIENT_CONNECT_CONNECTION)
	protected ClientConnection connection = null;

	@ActionFieldAttribute(name = socketName, mandatory = true, constantDescription = R.CLIENT_CONNECT_SOCKET)
	protected Socket socket = null;

	@Override
	protected void helpToAddParametersDerived(List<ReadableValue> list, Context context, Parameters parameters) throws Exception
	{
		Helper.helpToAddParameters(list, ParametersKind.CONNECT, context, this.owner.getMatrix(), parameters, null, connectionName, null);
	}

	@Override
	protected HelpKind howHelpWithParameterDerived(Context context, Parameters parameters, String fieldName) throws Exception
	{
		return Helper.canFillParameter(this.owner.getMatrix(), context, parameters, null, connectionName, fieldName) ? HelpKind.ChooseFromList : null;
	}
	
	@Override
	protected void listToFillParameterDerived(List<ReadableValue> list, Context context, String parameterToFill, Parameters parameters) throws Exception
	{
		Helper.messageValues(list, context, this.owner.getMatrix(), parameters, null, connectionName, null, parameterToFill);
	}

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		boolean res = this.connection.getClient().connect(context, this.socket, parameters.select(TypeMandatory.Extra).makeCopy());
		if (res)
		{
			super.setResult(null);
		}
		else
		{
			super.setError("Connection can not be established.", ErrorKind.CLIENT_ERROR);
		}
	}
}
