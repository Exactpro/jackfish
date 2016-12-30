////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.report;

import java.util.Date;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;

@ActionAttribute(
		group					= ActionGroups.Report,
		generalDescription 		= "Finishes and closes the report.",
		additionFieldsAllowed 	= true
	)
public class ReportFinish extends AbstractAction 
{
    public final static String passedName = "Passed";
    public final static String failedName = "Failed";
    public final static String reportName = "Report";
	public final static String startTimeName 	= "StartTime";
	public final static String finishTimeName 	= "FinishTime";

    @ActionFieldAttribute(name = reportName, mandatory = true, description = "The report object.")
    protected ReportBuilder    report;

    @ActionFieldAttribute(name = passedName, mandatory = true, description = "How many steps passed.")
    protected Integer          passed;

    @ActionFieldAttribute(name = failedName, mandatory = true, description = "How many steps failed.")
    protected Integer          failed;

	@ActionFieldAttribute(name = startTimeName, mandatory = false, description = "Reports given start time to the report. If null or absent - current time will be used.")
	protected Date startTime; 

	@ActionFieldAttribute(name = finishTimeName, mandatory = false, description = "Reports given start time to the report. If null or absent - current time will be used.")
	protected Date finishTime; 

	public ReportFinish()
	{
	}
	
	@Override
	public void initDefaultValues() 
	{
		this.startTime = null;
		this.finishTime = null;
	}
	
	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
	    
	    if (this.report == null)
	    {
	        super.setError(reportName, ErrorKind.EMPTY_PARAMETER);
	        return;
	    }
		
	    this.report.itemFinished(this.owner.getMatrix().getRoot(), 0, null);
	    this.report.reportFinished(this.failed, this.passed, this.startTime, this.finishTime);
	    
		super.setResult(null);
	}
}
