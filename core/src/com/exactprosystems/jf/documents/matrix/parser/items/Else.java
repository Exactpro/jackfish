////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.matrix.parser.items;

import com.csvreader.CsvWriter;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.DisplayDriver;
import com.exactprosystems.jf.documents.matrix.parser.SearchHelper;
import com.exactprosystems.jf.documents.matrix.parser.Tokens;

import java.util.List;

@MatrixItemAttribute(
		description 	= "This operator describes a line of the operator If which is performed if condition  - false." +
							" Line Else is only one for If.",
		examples 		= "{{#\n" +
							"#Id;#Let\n" +
							"year;new DateTime().getYears(new Date())\n" +
							"#If\n" +
							"year == 2017\n" +
							"#Action;#today\n" +
							"Print;'is 2017'\n" +
							"#Else\n" +
							"#Action;#today\n" +
							"Print;'is not 2017'\n" +
							"#EndIf#}}",
		shouldContain 	= { Tokens.Else },
		mayContain 		= { Tokens.Off, Tokens.RepOff }, 
		parents			= { If.class }, 
        real			= true,
		hasValue 		= false, 
		hasParameters 	= false,
        hasChildren 	= true,
		seeAlsoClass 	= {If.class}
	)
public class Else extends MatrixItem
{
    public Else()
    {
        super();
    }

	@Override
	protected MatrixItem makeCopy()
	{
		return new Else();
	}

	@Override
	protected Object displayYourself(DisplayDriver driver, Context context)
	{
		Object layout = driver.createLayout(this, 2);
		driver.showComment(this, layout, 0, 0, getComments());
		driver.showTitle(this, layout, 1, 0, Tokens.Else.get(), context.getFactory().getSettings());

		return layout;
	}

	@Override
	protected void writePrefixItSelf(CsvWriter writer, List<String> firstLine, List<String> secondLine)
	{
		super.addParameter(firstLine, TypeMandatory.System, Tokens.Else.get());
	}

    @Override
    protected boolean matchesDerived(String what, boolean caseSensitive, boolean wholeWord)
    {
        return SearchHelper.matches(Tokens.Else.get(), what, caseSensitive, wholeWord);
    }
}
