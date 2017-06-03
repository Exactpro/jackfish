////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.app;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.app.AppConnection;
import com.exactprosystems.jf.api.app.IApplication;
import com.exactprosystems.jf.api.common.ParametersKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameter;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.items.TypeMandatory;
import com.exactprosystems.jf.functions.HelpKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionAttribute(
		group = ActionGroups.App,
		suffix = "APPNI",
		generalDescription = "Plug-in dependent action. The action is used to open new windows in the web browser.",
		additionFieldsAllowed = true,
		additionalDescription = "The parameters are determined by the chosen plug-in. For example, the available"
				+ " parameters for win.jar are the following: "
				+ "{{` {{$URL$}} – string. It defines the URL.`}} "
				+ "{{` {{$Browser$}} - string. It defines which browser should be launched.`}} "
				+ "The parameters can be chosen in the dialogue"
				+ " window opened with the context menu of this action in {{$“All parameters”$}} option.",
		examples = "{{##Id;#Action;#Browser;#URL;#AppConnection\n"
				+ "APPNI1;ApplicationNewInstance;'Chrome';'http://google.com';app#}}",
		seeAlsoClass = {ApplicationStart.class, ApplicationConnectTo.class}
)
public class ApplicationNewInstance extends AbstractAction
{
	public static final String connectionName = "AppConnection";

	@ActionFieldAttribute(name = connectionName, mandatory = true, description = "A special object which identifies"
			+ " the started application session. This object is required in many other actions to specify the "
			+ "session of the application the indicated action belongs to. It is the output value of such actions "
			+ "as {{@ApplicationStart@}}, {{@ApplicationConnectTo@}}." )
	protected AppConnection connection	= null;

	@Override
	protected void helpToAddParametersDerived(List<ReadableValue> list, Context context, Parameters parameters) throws Exception
	{
		Helper.helpToAddParameters(list, ParametersKind.NEW_INSTANCE, this.owner.getMatrix(), context, parameters, null, connectionName);
	}

	@Override
	protected HelpKind howHelpWithParameterDerived(Context context, Parameters parameters, String fieldName) throws Exception
	{
		boolean res;
		switch (fieldName)
		{
			default:
				res = Helper.canFillParameter(this.owner.getMatrix(), context, parameters, null, connectionName, fieldName);
				break;
		}	
		
		return res ? HelpKind.ChooseFromList : null;
	}
	
	@Override
	protected void listToFillParameterDerived(List<ReadableValue> list, Context context, String parameterToFill, Parameters parameters) throws Exception
	{
		Helper.fillListForParameter(list, this.owner.getMatrix(), context, parameters, null, connectionName, parameterToFill);
	}
	
	@Override
	protected void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		Map<String, String> args = new HashMap<>();
		for (Parameter parameter : parameters.select(TypeMandatory.Extra))
		{
			args.put(parameter.getName(), String.valueOf(parameter.getValue()));
		}
		IApplication app = this.connection.getApplication();
		app.service().newInstance(args);
		super.setResult(null);
	}
}
