////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2016, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.tool.git.merge;

public class GitMergeBean
{
	private final String fileName;

	public GitMergeBean(String fileName)
	{
		this.fileName = fileName;
	}

	public String getFileName()
	{
		return this.fileName;
	}
}
