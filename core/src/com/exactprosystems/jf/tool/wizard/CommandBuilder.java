package com.exactprosystems.jf.tool.wizard;

import com.exactprosystems.jf.api.wizard.WizardCommand;
import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.matrix.parser.Parser;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.tool.matrix.MatrixFx;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder
{
    private static final Logger logger = Logger.getLogger(CommandBuilder.class);

    private List<WizardCommand> commands = new ArrayList<>();
    
    private CommandBuilder()
    {}
    
    public static CommandBuilder start()
    {
        return new CommandBuilder();
    }
    
    public List<WizardCommand> build()
    {
        return this.commands;
    }
    
    public CommandBuilder addMatrixItem(MatrixFx matrix, MatrixItem where, MatrixItem what, int index)
    {
        this.commands.add(context -> 
        {
            matrix.insert(where, index, what);
            where.init(matrix);
        });
        return this;
    }
    
    public CommandBuilder removeMatrixItem(MatrixFx matrix, MatrixItem what)
    {
        this.commands.add(context -> 
        {
            matrix.remove(what);
        });
        return this;
    }

    public static MatrixItem create(Matrix matrix, String itemName, String actionName)
    {
        try
        {
            MatrixItem res = Parser.createItem(itemName, actionName);
            res.init(matrix);
            return res;
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}