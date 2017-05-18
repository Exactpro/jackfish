package com.exactprosystems.jf.common.documentation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.actions.ActionsList;
import com.exactprosystems.jf.api.app.ControlKind;
import com.exactprosystems.jf.api.app.OperationKind;
import com.exactprosystems.jf.common.ControlsAttributes;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportFactory;
import com.exactprosystems.jf.common.version.VersionInfo;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.guidic.controls.AbstractControl;
import com.exactprosystems.jf.documents.matrix.parser.Parser;
import com.exactprosystems.jf.documents.matrix.parser.items.ActionItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpActionItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpTable;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpChapter;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpPicture;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpText;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItemAttribute;
import com.exactprosystems.jf.documents.matrix.parser.items.TempItem;
import com.exactprosystems.jf.functions.Table;

public class DocumentationBuilder
{
    public static MatrixItem createHelp (ReportBuilder report, Context context) throws Exception
    {
        return null;
    }
    
    public static MatrixItem createUserManual (ReportBuilder report, Context context) throws Exception
    {
        MatrixItem help = new HelpChapter("Help", 1);

//        addChapter(help, "Introduction", 3);
        addText(help, DocumentationBuilder.class.getResourceAsStream("intro1.txt"));
        addPicture(help, "Architecture", DocumentationBuilder.class.getResourceAsStream("Intro.png"));
        addText(help, DocumentationBuilder.class.getResourceAsStream("intro2.txt"));
//        addChapter(help, "MVEL", 3);
//        addText(help, DocumentationBuilder.class.getResourceAsStream("mvel.txt"));
//        addChapter(help, "All controls", 3);
//        addAllControlsTable(help, "All controls", context);
//        addChapter(help, "Matrix syntax", 3);
//        addAllItems(help);
//        addChapter(help, "Actions by groups", 3);
//        addAllActions(help);
        
        return help;
    }
        
    public static MatrixItem createHelpForItem (ReportBuilder report, Context context, MatrixItem item) throws Exception
    {
        MatrixItem help = null;
        
        if (item instanceof ActionItem)
        {
            help = new HelpActionItem( ((ActionItem)item).getActionClass() );
        }
        else
        {
            help = new HelpItem(item.getClass());
        }
        
        return help;
    }
    
    public static void addPicture(MatrixItem root, String title, InputStream stream) throws Exception
    {
        MatrixItem picture = new HelpPicture(title, stream);
        root.insert(root.count(), picture);
    }
    
    
    public static void addAllControlsTable(MatrixItem root, String title, Context context) throws Exception
    {
        try
        {
            for (ControlKind controlKind : ControlKind.values())
            {
                Class<?> controlClass = Class.forName(AbstractControl.class.getPackage().getName() + "." + controlKind.getClazz());
                controlClass.getSimpleName();
            }
        
            List<String> headers = new ArrayList<>();
            headers.add("#");
            
            for (OperationKind kind : OperationKind.values())
            {
                headers.add(kind.toString());
            }

            Table table = new Table(headers.toArray(new String[] {}), context.getEvaluator());
            for (int i = 1; i < table.getHeaderSize(); i++)
            {
                table.considerAsColored(table.getHeader(i));
            }

            for (ControlKind k : ControlKind.values())
            {
                String[] arr = new String[OperationKind.values().length + 1];
                
                Class<?> controlClass = Class.forName(AbstractControl.class.getPackage().getName() +"."+ k.getClazz());
                arr[0] = controlClass.getSimpleName();
                
                ControlsAttributes annotation = controlClass.getAnnotation(ControlsAttributes.class);
                OperationKind defaultOperation = annotation.bindedClass().defaultOperation();
                int count = 1; 
                for (OperationKind kind : OperationKind.values())
                {
                    if (annotation.bindedClass().isAllowed(kind))
                    {
                        boolean isDefaultOperation = kind == defaultOperation;
                        if (isDefaultOperation)
                        {
                            arr[count] = "+|RED";
                        }
                        else
                        {
                            arr[count] = "+";
                        }
                    }
                    else
                    {
                        arr[count] = "";
                    }
                    count++;
                }
                table.addValue(arr);
            }
            
            MatrixItem tableItem = new HelpTable(title, table);
            root.insert(root.count(), tableItem);
        }
        catch (Exception e)
        {
        }
    }
    
    public static void addChapter(MatrixItem root, String title, int level) throws Exception
    {
        MatrixItem chapter = new HelpChapter(title, level);
        root.insert(root.count(), chapter);
    }
    
    public static void addText(MatrixItem root, InputStream stream) throws Exception
    {
        MatrixItem text = new HelpText(stream);
        root.insert(root.count(), text);
    }
    
    @SuppressWarnings("unchecked")
    public static void addAllItems(MatrixItem root) throws Exception
    {
        MatrixItem item = new HelpChapter("Matrix syntax", 2);
        root.insert(root.count(), item);

        for (Class<?> clazz : Parser.knownItems)
        {
            MatrixItemAttribute attribute = clazz.getAnnotation(MatrixItemAttribute.class);
            if (attribute == null)
            {
                continue;
            }

            if (!attribute.real() || clazz.equals(ActionItem.class) || clazz.equals(TempItem.class))
            {
                continue;
            }

            item.insert(item.count(), new HelpItem((Class<? extends MatrixItem>) clazz));
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void addAllActions(MatrixItem root)
    {
        MatrixItem item = new HelpChapter("All actions by groups", 2);
        root.insert(root.count(), item);

        Map<Class<?>, ActionGroups> map = new HashMap<>();
        for (Class<?> action : ActionsList.actions)
        {
            map.put(action, action.getAnnotation(ActionAttribute.class).group());
        }

        for (ActionGroups groups : ActionGroups.values())
        {
            MatrixItem groupItem = new HelpChapter(groups.toString(), 3);
            item.insert(item.count(), groupItem);

            for (Map.Entry<Class<?>, ActionGroups> entry : map.entrySet())
            {
                if (entry.getValue() == groups)
                {
                    groupItem.insert(groupItem.count(), new HelpActionItem((Class<? extends AbstractAction>) entry.getKey()));
                }
            }
        }
    }
    
}