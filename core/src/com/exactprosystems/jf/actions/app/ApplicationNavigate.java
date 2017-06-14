////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.app;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.functions.HelpKind;

import java.util.List;

@ActionAttribute(
		group = ActionGroups.App,
		suffix = "APPPNVG",
		generalDescription = "Navigate inside application.",
		additionFieldsAllowed = true,
		additionalDescription = "The parameters are determined by the chosen plug-in. "
				+ "{{`For example, an additional"
				+ " parameter {{$Navigate$}} is available for web plug-in. It has two values {{$BACK$}} and {{$FORWARD$}} which help"
				+ " to move within the browser back and forward respectively.`}} "
				+ "The parameters can be chosen in the"
				+ " dialogue window opened with the context menu of this action in {{$'All parameters'$}} option.",
		examples =
				"{{##Action;#Navigate;#Navigate;#AppConnection\n"
				+ "ApplicationNavigate;NavigateKind.BACK;NavigateKind.FORWARD;app#}}",
		seeAlsoClass = {ApplicationStart.class, ApplicationConnectTo.class}
)
public class ApplicationNavigate extends AbstractAction
{
	public static final String connectionName = "AppConnection";
	public static final String navigateKindName = "Navigate";

	@ActionFieldAttribute(name = connectionName, mandatory = true, description = "A special object which identifies"
			+ " the started application session. This object is required in many other actions to specify the "
			+ "session of the application the indicated action belongs to. It is the output value of such actions"
			+ " as {{@ApplicationStart@}}, {{@ApplicationConnectTo@}}.")
	protected AppConnection connection = null;

	@ActionFieldAttribute(name = navigateKindName, mandatory = true, description = "Where navigate. See additional field description")
	protected NavigateKind kind = null;

	@Override
	protected void helpToAddParametersDerived(List<ReadableValue> list, Context context, Parameters parameters) throws Exception
	{
		list.add(new ReadableValue(navigateKindName, "Kind to navigation"));
	}

	@Override
	protected void listToFillParameterDerived(List<ReadableValue> list, Context context, String parameterToFill, Parameters parameters) throws Exception
	{
		switch (parameterToFill)
		{
			case navigateKindName:
				for (NavigateKind kind : NavigateKind.values())
				{
					list.add(new ReadableValue(NavigateKind.class.getSimpleName() + "." + kind.name()));
				}
				break;
		}
	}

	@Override
	protected HelpKind howHelpWithParameterDerived(Context context, Parameters parameters, String fieldName) throws Exception
	{
		switch (fieldName)
		{
			case navigateKindName:
				return HelpKind.ChooseFromList;

			default:
				return null;
		}

	}

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		IApplication app = this.connection.getApplication();
		IRemoteApplication service = app.service();
		service.navigate(this.kind);
		super.setResult(null);
	}
}
