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
import com.exactprosystems.jf.api.conditions.Condition;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.items.RawTable;
import com.exactprosystems.jf.documents.matrix.parser.items.TypeMandatory;
import com.exactprosystems.jf.functions.Table;

import java.util.List;

@ActionAttribute(
		group						  = ActionGroups.Tables,
		suffix						  = "TBLIDX",
		constantGeneralDescription 	  = R.TABLE_GET_ROW_INDEXES_GENERAL_DESC,
		additionFieldsAllowed 		  = true,
		constantAdditionalDescription = R.TABLE_GET_ROW_INDEXES_ADDITIONAL_DESC,
		constantOutputDescription	  = R.TABLE_GET_ROW_INDEXES_OUTPUT_DESC ,
		outputType					  = List.class,
		constantExamples 			  = R.TABLE_GET_ROW_INDEXES_EXAMPLE,
		seeAlsoClass				  = {RawTable.class, TableLoadFromFile.class, TableLoadFromDir.class, TableCreate.class, TableSelect.class}
	)
public class TableGetRowIndexes extends AbstractAction 
{
	public static final String tableName = "Table";

	@ActionFieldAttribute(name = tableName, mandatory = true, constantDescription = R.TABLE_GET_ROW_INDEXES_TABLE)
	protected Table table;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		Parameters extra = parameters.select(TypeMandatory.Extra);

		Condition[] conditions = Condition.convertToCondition(extra.makeCopy());
		List<Integer> indexes = this.table.findAllIndexes(conditions);

		super.setResult(indexes);
	}
}