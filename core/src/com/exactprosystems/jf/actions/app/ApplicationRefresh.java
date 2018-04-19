/*******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactprosystems.jf.actions.app;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.api.app.AppConnection;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

@ActionAttribute(
		group					   = ActionGroups.App,
		suffix					   = "APPR",
		constantGeneralDescription = R.APP_REFRESH_GENERAL_DESC,
		additionFieldsAllowed      = false,
		constantExamples       	   = R.APP_REFRESH_EXAMPLE,
		seeAlsoClass 			   = {ApplicationStart.class, ApplicationConnectTo.class}
	)
public class ApplicationRefresh extends AbstractAction
{
	public static final String connectionName = "AppConnection";

	@ActionFieldAttribute(name = connectionName, mandatory = true, constantDescription = R.APPLICATION_REFRESH_CONNECTION)
	protected AppConnection connection;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		Helper.getApplication(this.connection).service().refresh();
		super.setResult(null);
	}
}
