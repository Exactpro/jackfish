////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.api.service;

import java.util.HashSet;
import java.util.Set;

public class ServiceHelper
{
	public static Set<ServicePossibility> possebilities(Class<?> clazz)
	{
		Set<ServicePossibility> res = new HashSet<ServicePossibility>();
		ServiceAttribute attr = clazz.getAnnotation(ServiceAttribute.class);
		for (ServicePossibility possibility : attr.possibilities())
		{
			res.add(possibility);
		}
		return res;
	}
	
	public static void errorIfDisable(Class<?> clazz, ServicePossibility possibility) throws ServicePossibilityIsDisabled
	{
		ServiceAttribute attr = clazz.getAnnotation(ServiceAttribute.class);
		for (ServicePossibility poss : attr.possibilities())
		{
			if (poss == possibility)
			{
				return;
			}
		}
		
		throw new ServicePossibilityIsDisabled("For client " + clazz.getSimpleName() + " possibility " + possibility.getDescription() 
				+ " is not allowed.");
	}
	

}
