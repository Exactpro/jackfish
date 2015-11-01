////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.app;

public interface IControl
{
	ControlKind 		getBindedClass();

	ISection 			getSection();
	void 				setSection(ISection section);

	String 				getID();
	String 				getOwnerID();
	String 				getUID();
	String 				getXpath();
	boolean				useAbsoluteXpath();
	String 				getClazz();
	String 				getName();
	String 				getTitle();
	String 				getAction();
	String 				getText();
	String 				getTooltip();
	String 				getExpression();
	String 				getRowsId();
	String 				getHeaderId();
	boolean 			isWeak();
	boolean				useNumericHeader();
	int					getTimeout();
	Addition 			getAddition();
	Visibility			getVisibility();

	Locator				locator();
	
	void prepare(Part operationPart, Object value)  throws Exception;
	OperationResult operate(IRemoteApplication remote, IWindow window, Object value)  throws Exception;
}
