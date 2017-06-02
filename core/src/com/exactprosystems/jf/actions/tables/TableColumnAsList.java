////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.tables;

import com.exactprosystems.jf.actions.*;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.functions.HelpKind;
import com.exactprosystems.jf.functions.Table;

import java.util.List;

@ActionAttribute(
		group					= ActionGroups.Tables,
		suffix					= "TBL",
		generalDescription 		= "This action extract one column of a table and returns it as a list of values.",
		additionFieldsAllowed 	= false,
		outputDescription 		= "List of strings.",
		outputType				= List.class,
		examples 				= "",
		seeAlsoClass = { Table.class }
	)
public class TableColumnAsList extends AbstractAction 
{
    public final static String tableName = "Table";
    public final static String columnName = "Column";
    public final static String getValuesName = "GetValues";

    @ActionFieldAttribute(name = tableName, mandatory = true, description = "A table from which the column will be extracted.")
    protected Table table = null;

    @ActionFieldAttribute(name = columnName, mandatory = true, description = "Column name which will be extracted.")
    protected String column = null;

    @ActionFieldAttribute(name = getValuesName, mandatory = false, description = "If true get values otherwise get expressions.")
    protected Boolean getValues;


    @Override
    protected HelpKind howHelpWithParameterDerived(Context context, Parameters parameters, String fieldName) throws Exception
    {
        switch (fieldName)
        {
            case getValuesName:
                return HelpKind.ChooseFromList;
        }
        
        return null;
    }

    
    @Override
    protected void listToFillParameterDerived(List<ReadableValue> list, Context context, String parameterToFill, Parameters parameters) throws Exception
    {
        switch (parameterToFill)
        {
            case getValuesName:
                list.add(ReadableValue.TRUE);
                list.add(ReadableValue.FALSE);
                break;
            default:
        }
    }

    @Override
    public void initDefaultValues() 
    {
        this.getValues = false;
    }
    
	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		if (!this.table.columnIsPresent(this.column))
		{
			setError(String.format("Columns with name '%s' is not presented in table", this.column), ErrorKind.WRONG_PARAMETERS);
			return;
		}
		super.setResult(this.table.getColumnAsList(this.column, this.getValues));
	}
}