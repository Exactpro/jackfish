////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.config;

import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.common.IContext;
import com.exactprosystems.jf.api.common.IMatrixRunner;
import com.exactprosystems.jf.common.MatrixRunner;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixRoot;
import com.exactprosystems.jf.documents.matrix.parser.items.SubCase;
import com.exactprosystems.jf.documents.matrix.parser.listeners.IMatrixListener;

import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;

public class Context implements IContext, AutoCloseable, Cloneable
{
	protected Context(IMatrixListener matrixListener, PrintStream out, Configuration configuration) throws Exception
	{
		this.configuration = configuration;
		this.evaluator = configuration.createEvaluator();

		this.matrixListener = matrixListener;
		this.outStream = out;
	}

	@Override
	public IMatrixRunner createRunner(Reader reader, Date startTime, Object parameter) throws Exception
	{
		return new MatrixRunner(this, reader, startTime, parameter);
	}

	@Override
	public Context clone() throws CloneNotSupportedException
	{
		try
		{
			Context clone = ((Context) super.clone());

			clone.configuration = this.configuration;
			clone.matrixListener = this.matrixListener.clone();
			clone.outStream = this.outStream;
			clone.evaluator = configuration.createEvaluator();
			clone.libs = new HashMap<String, Matrix>();

			return clone;
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw new InternalError();
		}
	}

	public Configuration getConfiguration()
	{
		return this.configuration;
	}

	public AbstractEvaluator getEvaluator()
	{
		return this.evaluator;
	}

	public PrintStream getOut()
	{
		return this.outStream;
	}

	public IMatrixListener getMatrixListener()
	{
		return this.matrixListener;
	}

	@Override
	public void close() throws Exception
	{
	}

	public SubCase referenceToSubcase(String name, MatrixItem item)
	{
		MatrixItem ref = item.findParent(MatrixRoot.class).find(true, SubCase.class, name);

		if (ref != null && ref instanceof SubCase)
		{
			return (SubCase) ref;
		}
		if (name == null)
		{
			return null;
		}
		String[] parts = name.split("\\.");
		if (parts.length < 2)
		{
			return null;
		}
		String ns = parts[0];
		String id = parts[1];

		Matrix matrix = this.libs.get(ns);
		if (matrix == null)
		{
			matrix = this.configuration.getLib(ns);

			if (matrix == null)
			{
				return null;
			}
			try
			{
				matrix = matrix.clone();
			}
			catch (CloneNotSupportedException e)
			{
				logger.error(e.getMessage(), e);
			}
			this.libs.put(ns, matrix);
		}

		return (SubCase) matrix.getRoot().find(true, SubCase.class, id);
	}

	public List<ReadableValue> subcases(MatrixItem item)
	{
		final List<ReadableValue> res = new ArrayList<ReadableValue>();

		MatrixItem root = item.findParent(MatrixRoot.class);

		root.bypass(it ->
		{
			if (it instanceof SubCase)
			{
				res.add(new ReadableValue(it.getId(), ((SubCase) it).getName()));
			}
		});
		for (Entry<String, Matrix> entry : this.configuration.getLibs().entrySet())
		{
			final String name = entry.getKey();
			Matrix lib = entry.getValue();

			if (lib != null)
			{
				lib.getRoot().bypass(it ->
				{
					if (it instanceof SubCase)
					{
						res.add(new ReadableValue(name + "." + it.getId(), ((SubCase) it).getName()));
					}
				});
			}
		}

		return res;
	}

	private Configuration			configuration;
	private AbstractEvaluator		evaluator;
	private IMatrixListener			matrixListener	= null;
	private PrintStream				outStream		= null;
	private Map<String, Matrix>		libs 			= new HashMap<>();

	private static final Logger	logger				= Logger.getLogger(Context.class);
}
