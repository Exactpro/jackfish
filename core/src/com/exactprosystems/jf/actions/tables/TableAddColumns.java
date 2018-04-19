/*******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactprosystems.jf.actions.tables;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.functions.Table;

@ActionAttribute(
		group					  	= ActionGroups.Tables,
		constantGeneralDescription 	= R.TABLE_ADD_COLUMNS_GENERAL_DESC,
		additionFieldsAllowed 	  	= false,
		constantExamples   			= R.TABLE_ADD_COLUMNS_EXAMPLES,
		seeAlsoClass 			    = {TableReplace.class, TableColumnRename.class, TableAddValue.class, TableConsiderColumnsAs.class}
	)
public class TableAddColumns extends AbstractAction 
{
	public static final String tableName   = "Table";
	public static final String columnsName = "Columns";
	public static final String indexName   = "Index";

	@ActionFieldAttribute(name = tableName, mandatory = true, constantDescription = R.TABLE_ADD_COLUMNS_TABLE)
	protected Table table;

	@ActionFieldAttribute(name = columnsName, mandatory = true, constantDescription = R.TABLE_ADD_COLUMNS_COLUMNS)
	protected String[] columns;

	@ActionFieldAttribute(name = indexName, mandatory = false, constantDescription = R.TABLE_ADD_COLUMNS_INDEX)
	protected Integer index;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		for (String column : this.columns)
		{
			if (column.isEmpty())
			{
				super.setError("Column name can't be empty", ErrorKind.EMPTY_PARAMETER);
				return;
			}
		}

		if (this.index != null)
		{
			if (this.index < 0 || this.index > this.table.getHeaderSize())
			{
				super.setError("Index is out of bounds", ErrorKind.WRONG_PARAMETERS);
				return;
			}
			this.table.addColumns(this.index, this.columns);
		}
		else
		{
			this.table.addColumns(this.columns);
		}

		super.setResult(null);
	}
}


