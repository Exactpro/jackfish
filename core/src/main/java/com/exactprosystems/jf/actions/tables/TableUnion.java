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

package com.exactprosystems.jf.actions.tables;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.functions.Table;

@ActionAttribute(
		group 					   = ActionGroups.Tables,
		constantGeneralDescription = R.TABLE_UNION_GENERAL_DESC,
		additionFieldsAllowed 	   = false,
		constantExamples           = R.TABLE_UNION_EXAMPLE
)
public class TableUnion extends AbstractAction
{
	public static final String mainTableName   = "MainTable";
	public static final String unitedTableName = "UnitedTable";

	@ActionFieldAttribute(name = mainTableName, mandatory = true, constantDescription = R.TABLE_UNION_MAIN_TABLE)
	protected Table mainTable;

	@ActionFieldAttribute(name = unitedTableName, mandatory = true, constantDescription = R.TABLE_UNION_UNITED_TABLE)
	protected Table unitedTable;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		boolean atLeastOneCoincidence = false;
		for (int mainNum = 0; mainNum < this.mainTable.getHeaderSize(); mainNum++)
		{
			for (int unitedNum = 0; unitedNum < this.unitedTable.getHeaderSize(); unitedNum++)
			{
				if (this.mainTable.getHeader(mainNum).equals(this.unitedTable.getHeader(unitedNum)))
				{
					atLeastOneCoincidence = true;
					break;
				}
			}
		}

		if (atLeastOneCoincidence)
		{
			this.mainTable.addAll(this.unitedTable);
		}

		super.setResult(null);
	}
}
