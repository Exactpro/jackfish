package com.exactprosystems.jf.common.documentation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.exactprosystems.jf.actions.AbstractAction;
import com.exactprosystems.jf.actions.ActionAttribute;
import com.exactprosystems.jf.actions.ActionGroups;
import com.exactprosystems.jf.actions.ActionsList;
import com.exactprosystems.jf.api.app.ControlKind;
import com.exactprosystems.jf.api.app.IApplicationFactory;
import com.exactprosystems.jf.api.app.OperationKind;
import com.exactprosystems.jf.api.common.DateTime;
import com.exactprosystems.jf.api.wizard.Wizard;
import com.exactprosystems.jf.api.wizard.WizardManager;
import com.exactprosystems.jf.common.ControlsAttributes;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.version.VersionInfo;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.guidic.controls.AbstractControl;
import com.exactprosystems.jf.documents.matrix.parser.Parser;
import com.exactprosystems.jf.documents.matrix.parser.items.ActionItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpActionItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpClass;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpTable;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpContent;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpItem;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpPicture;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpText;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpTextLine;
import com.exactprosystems.jf.documents.matrix.parser.items.HelpWizardItem;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItemAttribute;
import com.exactprosystems.jf.documents.matrix.parser.items.TempItem;
import com.exactprosystems.jf.functions.Content;
import com.exactprosystems.jf.functions.Table;

public class DocumentationBuilder
{
    public static MatrixItem createHelp (ReportBuilder report, Context context) throws Exception
    {
        AbstractEvaluator evaluator = context.getEvaluator();

        MatrixItem help = new HelpTextLine("");
        addContent(help, "", new Content());
        return help;
        //mvel
        //controls
        //syntax
        //items
        //actions
        //other classes
        //todo content
    }
    
    public static MatrixItem createUserManual (ReportBuilder report, Context context) throws Exception
    {
        AbstractEvaluator evaluator = context.getEvaluator();
        
        MatrixItem help = new HelpTextLine("{{``}}");

        String[][] table1 = new String[][]
                {
                        { "Version",  VersionInfo.getVersion() },
                        { "Release date", DateTime.current().str("dd MMM yyyy")}
                };

        String[][] table2 = new String[][]
                {
                    { "{{*Date*}}", "{{*Version*}}", "{{*By*}}", "{{*Comments*}}" },
                    { DateTime.current().str("dd MMM yyyy"), VersionInfo.getVersion(), "Valery Florov", "Initial Draft" }
                };
                
        String[][] table3 = new String[][]
                {
                    { "{{*Abbreviation*}}", "{{*Meaning*}}" },
                    { "JF", "JackFish" }
                };

        List<OperationKind> operations = Arrays.stream(OperationKind.values()).collect(Collectors.toList());
        int size = operations.size();

        addTextLine(help, "\\JackFishHuge");
        addTextLine(help, "{{``}}");
        addPicture(help, "", 100, DocumentationBuilder.class.getResourceAsStream("BurntOrangeLine.png"));
        addTable(help, "\\UserGuide",              false,  table1, new int[] { 50, 50 },  evaluator);
        for (int i = 0; i < 5; i++){
            addTextLine(help, "{{` `}}");
        }

        addPicture(help, "", 100, DocumentationBuilder.class.getResourceAsStream("BurntOrangeLine.png"));
        addTable(help, "\\DocInfo",    false,  table2, new int[] { 25, 23, 23, 25 },  evaluator);
        addTable(help, "\\Abbreviations",           false,  table3, new int[] { 50, 50 },  evaluator);
        addTextLine(help, "{{&&}}");
        
        addContent(help, "{{*Table of contenst*}}", new Content());
        addTextLine(help, "{{&&}}");

        addText(help, DocumentationBuilder.class.getResourceAsStream("intro1.txt"));
        addPicture(help, "Architecture", 80, DocumentationBuilder.class.getResourceAsStream("Intro.png"));
        addText(help, DocumentationBuilder.class.getResourceAsStream("intro2.txt"));
        addTextLine(help, "{{&&}}");
        
        addTextLine(help, "{{5MVEL5}}");
        addText(help, DocumentationBuilder.class.getResourceAsStream("mvel.txt"));
        addTextLine(help, "{{&&}}");

        addTextLine(help, "{{5All controls5}}");
        addText(help, DocumentationBuilder.class.getResourceAsStream("controls.txt"));
        addTextLine(help, "{{&&}}");
        addAllControlsTable(help, "All controls", context, operations.subList(0, size/3), true, true);
        addTextLine(help, "{{&&}}");
        addAllControlsTable(help, "All controls - continue", context, operations.subList(size/3, size*2/3), true, true);
        addTextLine(help, "{{&&}}");
        addAllControlsTable(help, "All controls - end", context, operations.subList(size*2/3, size), true, true);
        addTextLine(help, "{{&&}}");

        addTextLine(help, "{{5Matrix syntax5}}");
        addAllItems(help);
        addTextLine(help, "{{&&}}");

        addAllActions(help);
        addTextLine(help, "{{&&}}");

        //todo
//        addClass(help, Do.class);
        
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
    
    public static MatrixItem createHelpForWizard(ReportBuilder report, Context context, Class<? extends Wizard> clazz)
    {
        WizardManager manager = context.getFactory().getWizardManager();
        MatrixItem help = new HelpWizardItem(manager, clazz);
        return help;
    }
    
    public static MatrixItem createHelpForPlugin(ReportBuilder report, Context context, String title, IApplicationFactory applicationFactory) throws Exception
    {
        InputStream stream = applicationFactory.getHelp();
        MatrixItem help = new HelpTextLine("{{`{{*" + title + "*}}`}}"); 
        addText(help, stream);
        addTextLine(help, "{{`{{*Supported controls*}}`}}");
        Set<ControlKind> controls = applicationFactory.supportedControlKinds();
        String s = controls.stream().map(c -> "{{@" + c.getClazz() + "@}}").collect(Collectors.joining(", "));
        addTextLine(help, "{{`" + s + "`}}");
        
        return help;
    }

    public static void addContent(MatrixItem root, String title, Content content) throws Exception
    {
        MatrixItem contentItem = new HelpContent(title, content); 
        root.insert(root.count(), contentItem);
    }
    
    public static void addPicture(MatrixItem root, String title, int width, InputStream stream) throws Exception
    {
        MatrixItem picture = new HelpPicture(title, stream, width); 
        root.insert(root.count(), picture);
    }
    
    public static void addTable(MatrixItem root, String title, boolean bordered, String[][] content, int[] widths, AbstractEvaluator evaluator) throws Exception
    {
        Table table = new Table(content, evaluator);
        MatrixItem text = new HelpTable(title, table, bordered, widths);
        root.insert(root.count(), text);
    }
    
    public static void addAllControlsTable(MatrixItem root, String title, Context context, List<OperationKind> operations, boolean rotate, boolean bordered) throws Exception
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
            
            for (OperationKind kind : operations)
            {
                if (rotate)
                {
                    headers.add("{{^" + kind.toString() + "^}}");
                }
                else
                {
                    headers.add(kind.toString());
                }
            }

            Table table = new Table(headers.toArray(new String[] {}), context.getEvaluator());
            for (int i = 1; i < table.getHeaderSize(); i++)
            {
                table.considerAsColored(table.getHeader(i));
            }

            for (ControlKind k : ControlKind.values())
            {
                String[] arr = new String[operations.size() + 1];
                
                Class<?> controlClass = Class.forName(AbstractControl.class.getPackage().getName() +"."+ k.getClazz());
                arr[0] = controlClass.getSimpleName();
                
                ControlsAttributes annotation = controlClass.getAnnotation(ControlsAttributes.class);
                OperationKind defaultOperation = annotation.bindedClass().defaultOperation();
                int count = 1; 
                for (OperationKind kind : operations)
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
            
            MatrixItem tableItem = new HelpTable(title, table, bordered, new int[] {}); 
            root.insert(root.count(), tableItem);
        }
        catch (Exception e)
        {
        }
    }
    
    public static void addTextLine(MatrixItem root, String str) throws Exception
    {
        MatrixItem line = new HelpTextLine(str);
        root.insert(root.count(), line);
    }
    
    public static void addText(MatrixItem root, InputStream stream) throws Exception
    {
        MatrixItem text = new HelpText(stream);
        root.insert(root.count(), text);
    }
    
    public static void addClass(MatrixItem root, Class<?> clazz) throws Exception
    {
        MatrixItem item = new HelpClass(clazz);
        root.insert(root.count(), item);
    }

    @SuppressWarnings("unchecked")
    public static void addAllItems(MatrixItem root) throws Exception
    {
        MatrixItem item = new HelpTextLine("{{4Items4}}");
        root.insert(root.count(), item);

        for (Class<?> clazz : Parser.knownItems)
        {
            Deprecated deprecated = clazz.getAnnotation(Deprecated.class);
            if (deprecated != null)
            {
                continue;
            }
            
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
        MatrixItem item = new HelpTextLine("{{5All actions by groups5}}");
        root.insert(root.count(), item);

        Map<Class<?>, ActionGroups> map = new HashMap<>();
        for (Class<?> action : ActionsList.actions)
        {
            map.put(action, action.getAnnotation(ActionAttribute.class).group());
        }

        for (ActionGroups groups : ActionGroups.values())
        {
            MatrixItem groupItem = new HelpTextLine("{{4" + groups.toString() + "4}}");
            item.insert(item.count(), groupItem);

            for (Map.Entry<Class<?>, ActionGroups> entry : map.entrySet())
            {
                Deprecated deprecated = entry.getKey().getAnnotation(Deprecated.class);
                if (deprecated != null)
                {
                    continue;
                }
                
                if (entry.getValue() == groups)
                {
                    groupItem.insert(groupItem.count(), new HelpActionItem((Class<? extends AbstractAction>) entry.getKey()));
                }
            }
        }
    }

}
