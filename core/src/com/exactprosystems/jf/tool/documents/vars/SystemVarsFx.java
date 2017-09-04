////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.tool.documents.vars;

import com.exactprosystems.jf.common.undoredo.Command;
import com.exactprosystems.jf.documents.DocumentFactory;
import com.exactprosystems.jf.documents.matrix.parser.Parameter;
import com.exactprosystems.jf.documents.vars.SystemVars;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import javafx.scene.control.ButtonType;
import javafx.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SystemVarsFx extends SystemVars
{
	public SystemVarsFx(String fileName, DocumentFactory factory) throws Exception
	{
		super(fileName, factory);
	}

	//==============================================================================================================================
	// Document
	//==============================================================================================================================
	@Override
	public void display() throws Exception
	{
		super.display();

		getParameters().fire();
	}

	@Override
	public void save(String fileName) throws Exception
	{
		super.save(fileName);
	}

	@Override
	public boolean canClose() throws Exception
	{
		if (!super.canClose())
		{
			return false;
		}
		
		if (isChanged())
		{
			ButtonType desision = DialogsHelper.showSaveFileDialog(getNameProperty().get());
			if (desision == ButtonType.YES)
			{
				save(getNameProperty().get());
			}
			if (desision == ButtonType.CANCEL)
			{
				return false;
			}
		}
		
		return true;
	}


	//----------------------------------------------------------------------------------------------
	public void updateNameRow(int index, String newValue)
	{
		String lastName = getParameterByIndex(index).getName();
		Command undo = () -> 
		{
			getParameterByIndex(index).setName(lastName);
		};
		Command redo = () -> 
		{
			getParameterByIndex(index).setName(newValue);
		};
		addCommand(undo, redo);
	}

	public void updateExpressionRow(int index, String newValue)
	{
		String lastExpression = getParameterByIndex(index).getExpression();
		Command undo = () -> 
		{ 
			getParameterByIndex(index).setExpression(lastExpression); 
		};
		Command redo = () -> 
		{ 
			getParameterByIndex(index).setExpression(newValue); 
		};
		addCommand(undo, redo);
	}

	void updateDescriptionRow(int index, String newValue)
	{
		String lastDescription = getParameterByIndex(index).getDescription();
		Command undo = () ->
		{
			getParameterByIndex(index).setDescription(lastDescription);
		};
		Command redo = () ->
		{
			getParameterByIndex(index).setDescription(newValue);
		};
		addCommand(undo, redo);
	}

	public void addNewVariable() throws Exception
	{
		Command undo = () -> 
		{ 
			this.getParameters().remove(this.getParameters().size() - 1); 
		};
		Command redo = () -> 
		{ 
			this.getParameters().add("name", "'expression'"); 
		};
		addCommand(undo, redo);
	}

	public void removeParameters(List<Parameter> parameters)
	{
		List<Pair<Integer, Parameter>>  indexes = parameters.stream()
			.map(par -> new Pair<>(getParameters().getIndex(par), par))
			.sorted(Comparator.comparingInt(Pair::getKey)).collect(Collectors.toList());
		
		Command undo = () -> 
		{
			indexes.forEach(pair -> getParameters()
					.insert(pair.getKey(), pair.getValue().getName(), pair.getValue().getExpression(), pair.getValue().getType()));
		};
		Command redo = () -> 
		{
			for (int i = indexes.size() - 1; i >= 0; i--)
			{
				getParameters().remove(indexes.get(i).getKey().intValue());
			}
		};
		addCommand(undo, redo);
	}

    //==============================================================================================================================
    // AbstractDocument
    //==============================================================================================================================
    @Override
    protected void afterRedoUndo()
    {
        super.afterRedoUndo();
    }
	
	private Parameter getParameterByIndex(int index)
	{
		return getParameters().getByIndex(index);
	}
}