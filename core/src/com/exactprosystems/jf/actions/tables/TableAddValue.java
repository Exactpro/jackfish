////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.tables;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.common.Context;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.parser.Parameters;
import com.exactprosystems.jf.common.parser.items.TypeMandatory;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.functions.Table;

@ActionAttribute(
		group					= ActionGroups.Tables,
		generalDescription 		= "Adds new row into the table.",
		additionFieldsAllowed 	= true
	)
public class TableAddValue extends AbstractAction 
{
	public final static String tableName = "Table";
	public static final String indexName = "Index";

	@ActionFieldAttribute(name = tableName, mandatory = true, description = "The table.")
	protected Table 	table 	= null;

	@ActionFieldAttribute(name = indexName, mandatory = false, description = "Index in the table")
	protected Integer	index	= Integer.MIN_VALUE;

	public TableAddValue()
	{
	}
	
	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		table.addValue(index, parameters.select(TypeMandatory.Extra));
		
		super.setResult(null);
	}
}

