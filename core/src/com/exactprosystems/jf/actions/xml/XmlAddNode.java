////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.actions.xml;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionFieldAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.actions.DefaultValuePool;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.ErrorKind;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.documents.matrix.parser.items.TypeMandatory;
import com.exactprosystems.jf.functions.Xml;

@ActionAttribute(
		group					      = ActionGroups.XML,
		constantGeneralDescription    = R.XML_ADD_NODE_GENERAL_DESC,
		additionFieldsAllowed 	   	  = true,
		constantAdditionalDescription = R.XML_ADD_NODE_ADDITIONAL_DESC,
		constantExamples 			  = R.XML_ADD_NODE_EXAMPLE
	)
public class XmlAddNode extends AbstractAction 
{
	public final static String xmlName 		= "Xml";
	public final static String nodeNameName = "NodeName";
	public final static String contentName 	= "Content";
	public final static String newXML = "NewXML";

	@ActionFieldAttribute(name = xmlName, mandatory = true, constantDescription = R.XML_ADD_NODE_XML)
	protected Xml 		xml 	= null;

	@ActionFieldAttribute(name = nodeNameName, mandatory = false, def = DefaultValuePool.Null, constantDescription = R.XML_ADD_NODE_NODE_NAME)
	protected String 	nodeName;

	@ActionFieldAttribute(name = contentName, mandatory = false, def = DefaultValuePool.Null, constantDescription = R.XML_ADD_NODE_CONTENT_NAME)
	protected String 	content;

	@ActionFieldAttribute(name =  newXML, mandatory = false, def = DefaultValuePool.Null, constantDescription = R.XML_ADD_NODE_NEW_XML)
	protected Xml 		copiedXML;

	@Override
	public void doRealAction(Context context, ReportBuilder report, Parameters parameters, AbstractEvaluator evaluator) throws Exception
	{
		if (copiedXML == null)
		{
			if (this.nodeName == null)
			{
				setError("Parameter #" + nodeNameName + " can't be null", ErrorKind.EMPTY_PARAMETER);
				return;
			}
			this.xml.addNode(this.nodeName, this.content, parameters.select(TypeMandatory.Extra).makeCopy());
		}
		else
		{
			this.xml.addNode(copiedXML);
		}
		super.setResult(null);
	}
}