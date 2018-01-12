////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.documents.matrix.parser.items.help;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.HTMLhelper;
import com.exactprosystems.jf.common.report.HelpBuilder;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.Result;
import com.exactprosystems.jf.documents.matrix.parser.ReturnAndResult;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HelpActionItem extends MatrixItem
{
	private Class<? extends AbstractAction> actionClazz;

	public HelpActionItem(Class<? extends AbstractAction> itemClazz)
    {
        this.actionClazz = itemClazz;
    }

	@Override
	protected MatrixItem makeCopy()
	{
		return new HelpActionItem(this.actionClazz);
	}
    
    @Override
    public String getItemName()
    {
        return "";
    }
    
    public void actionReport(ReportBuilder report, MatrixItem item, Class<? extends AbstractAction> clazz)
    {
        ActionAttribute attr = clazz.getAnnotation(ActionAttribute.class);

        report.itemIntermediate(item);
        report.putMark(this.actionClazz.getSimpleName());
        report.outLine(this, null, "{{`{{3" + "Action " + this.actionClazz.getSimpleName() + "3}}`}}", null);
	    String generalDescription = attr.constantGeneralDescription().get();
	    report.outLine(this, null, "{{`" + generalDescription + "`}}", null);
        if (attr.additionFieldsAllowed())
        {
            report.outLine(this, null, "{{`{{*Additional fields - Yes*}}`}}", null);
			String additionalDescription = attr.constantAdditionalDescription().get();
			report.outLine(this, null, "{{`" + additionalDescription + "`}}", null);
        }
        else
        {
            report.outLine(this, null, "{{`{{*Additional fields - No*}}`}}", null);
        }
        report.outLine(this, null, "{{`{{*Examples*}}`}}", null);
		String examples = attr.constantExamples().get();
		if (report instanceof HelpBuilder){
            report.outLine(this, null, "{{`" + HTMLhelper.htmlescape(examples) + "`}}", null);
        }
        else
        {
            report.outLine(this, null, "{{`" + examples + "`}}", null);
        }
        if (attr.seeAlsoClass().length != 0)
        {
            report.outLine(this, null, "{{`{{*See also*}}`}}", null);
            String s = Arrays.stream(attr.seeAlsoClass()).map(c -> report.decorateLink(c.getSimpleName(), c.getSimpleName())).collect(Collectors.joining(", "));
            report.outLine(this, null, "{{`" + s + "`}}", null);
        }
        
        // Input
        Field[] fields = clazz.getDeclaredFields();
        ReportTable table = report.addTable("{{*Input:*}}", null, true, false, new int[] { 15, 15, 50, 5, 7 ,8 }, "Field name", "Field type",
                "Description", "Mandatory", "Default", "Should filled");

        for (Field f : fields)
        {
            ActionFieldAttribute annotation = f.getAnnotation(ActionFieldAttribute.class);
            if (annotation == null)
            {
                continue;
            }

            String fieldDescription = annotation.constantDescription().get();
	        if (annotation.mandatory())
            {
                table.addValues(annotation.name(), f.getType().getSimpleName(), fieldDescription, "Yes", "", "Yes");
            }
            else
            {
                table.addValues(annotation.name(), f.getType().getSimpleName(), fieldDescription, "No", annotation.def(), annotation.shouldFilled() ? "Yes" : "No");
            }
        }

        // Output
        table = report.addTable("{{*Output:*}}", null, true, false, new int[] { 30, 70 }, "Result type", "Description");
		String outputDescription = attr.constantOutputDescription().get();
		table.addValues(attr.outputType().getSimpleName(), outputDescription);

    }

    @Override
    protected ReturnAndResult executeItSelf(long start, Context context, IMatrixListener listener, AbstractEvaluator evaluator, ReportBuilder report, Parameters parameters)
    {
        try
        {
            if (this.actionClazz != null)
            {
                actionReport(report, this, this.actionClazz);
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            return new ReturnAndResult(start, Result.Failed, e.getMessage(), ErrorKind.EXCEPTION, this);
        }
        return new ReturnAndResult(start, Result.Passed); 
    }
}