package com.exactprosystems.jf.actions.app;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.app.*;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.items.ActionItem;

import java.util.List;

@ActionAttribute(
		group = ActionGroups.App,
		suffix = "APPPNVG",
		generalDescription = "Navigate inside application.",
		additionFieldsAllowed = false
)
public class ApplicationNavigate extends AbstractAction
{
	public static final String connectionName = "AppConnection";
	public static final String navigateKindName = "Navigate";

	@ActionFieldAttribute(name = connectionName, mandatory = true, description = "The application connection.")
	protected AppConnection connection = null;

	@ActionFieldAttribute(name = navigateKindName, mandatory = true, description = "Where navigate. See NavigateKind")
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
	protected ActionItem.HelpKind howHelpWithParameterDerived(Context context, Parameters parameters, String fieldName) throws Exception
	{
		switch (fieldName)
		{
			case navigateKindName:
				return ActionItem.HelpKind.ChooseFromList;

			default:
				return null;
		}

	}

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		if (this.connection == null)
		{
			super.setError("Connection is null", ErrorKind.EMPTY_PARAMETER);
		}

		if (this.kind == null)
		{
			super.setError("NavigateKind is null", ErrorKind.EMPTY_PARAMETER);
		}
		IApplication app = connection.getApplication();
		IRemoteApplication service = app.service();
		service.navigate(this.kind);
		super.setResult(null);
	}

	@Override
	public void initDefaultValues()
	{
	}
}