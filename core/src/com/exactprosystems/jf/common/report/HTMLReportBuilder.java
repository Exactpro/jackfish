////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.common.report;

import com.exactprosystems.jf.api.app.ImageWrapper;
import com.exactprosystems.jf.api.common.DateTime;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.charts.ChartBuilder;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.documents.matrix.parser.Result;
import com.exactprosystems.jf.documents.matrix.parser.items.CommentString;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.functions.Content;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class HTMLReportBuilder extends ReportBuilder 
{
	private static final long serialVersionUID = 8277698425881479782L;

	private static Integer chartCount = 0;
	private static final String reportExt = ".html";
	private static final DateFormat dateTimeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss_");
	private final int columnCount = 7;

	public HTMLReportBuilder()
	{
		super();
	}

	public HTMLReportBuilder(String outputPath, String matrixName, Date currentTime) throws IOException
	{
		super(outputPath, matrixName, currentTime);
	}

	@Override
	protected String postProcess(String result)
	{
		return super.postProcess(result);
	}

	@Override
	protected String decorateStyle(String value, String style)
	{
		return String.format("<div class=\"%s\">%s</div>", style, value);
	}

	@Override
	protected String decorateLink(String name, String link)
	{
		String res = String.format("<a href=\"%s\" target=\"_blank\">%s</a>",
				link,
				name);
		return res;
	}

	@Override
	protected String decorateExpandingBlock(String name, String content)
	{
		String res = String.format("<a href=\"\" onclick=\"obj=this.parentNode.childNodes[1].style; "
				+ "obj.display=(obj.display!='block')?'block':'none'; return false;\">%s</a>"
				+ "<div style='display: none;'>%s</div>",				
				name,
				content);
		return res;
	}

    @Override
    protected String decorateGroupCell(String content, int level, boolean isNode)
    {
        String res = null;
        if (isNode)
        {
            res = String.format("<a href=\"javascript:void(0)\" indent-level=\"%d\" class=\"group\">%s</a>",               
                    level,
                    content);
        }
        else
        {
            res = String.format("<span indent-level=\"%d\" class=\"group\">%s</span>",               
                    level,
                    content);
        }
        return res;
    }

    @Override
	protected String replaceMarker(String marker)
	{
		return HTMLhelper.htmlMarker(marker);
	}

	@Override
	protected String generateReportName(String outputPath, String matrixName, String suffix, Date date) 
	{
		if (matrixName.toLowerCase().endsWith(Configuration.matrixExt))
		{
			matrixName = matrixName.substring(0, matrixName.length()-Configuration.matrixExt.length());
		}
		synchronized (dateTimeFormatter)
		{
			return outputPath + File.separator + dateTimeFormatter.format(date) + matrixName + suffix + reportExt;
		}
	}
	
	@Override
	protected void putMark(ReportWriter writer, String mark) throws IOException
	{
		writer.fwrite(String.format("<div id=\"TC_%s\" ></div>", mark));
	}

	@Override
	protected String generateReportDir(String matrixName, Date date) throws IOException
	{
		if (matrixName.toLowerCase().endsWith(Configuration.matrixExt))
		{
			matrixName = matrixName.substring(0, matrixName.length()-Configuration.matrixExt.length());
		}
		synchronized (dateTimeFormatter)
		{
			return dateTimeFormatter.format(date) + matrixName;
		}
	}

	//region Global report
	@Override
	protected void reportHeader(ReportWriter writer, Date date, String version) throws IOException
	{
		writer.fwrite(
				"<html>\n" +
				"<head>\n" +
				"<title>Report</title>\n" +
				"<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n");

		writer.fwrite(
				"<script type='text/javascript'>\n" +
				"<!--\n");
		//TODO replace to new jquery
		writer.include(getClass().getResourceAsStream("jquery-1.8.3.min.js"));
		writer.include(getClass().getResourceAsStream("reports.js"));
		writer.fwrite(
				"-->\n" +
				"</script>\n");

		writer.fwrite("<script>\n");
		writer.include(getClass().getResourceAsStream("d3.min.js"));
		writer.fwrite("</script>\n");

		writer.fwrite("<script>\n");
		writer.include(getClass().getResourceAsStream("charts.js"));
		writer.fwrite("</script>\n");

		writer.fwrite(
				"<style>\n" +
				"<!--\n");
		writer.include(getClass().getResourceAsStream("style.css"));
		writer.fwrite(
				"-->\n" +
				"</style>\n");
		
		writer.fwrite(
				"</head>\n" +
				"<body>\n" +
				"<h1>EXECUTION REPORT</h1>\n" +
				"<table class='table'>\n");

		writer.fwrite("<tr><td><span id='name'></span>\n");
		writer.fwrite("<tr><td>Version <td>%s\n", Str.asString(version));
		writer.fwrite("<tr><td>Start time: <td><span id='startTime'>Calculating...</span>\n");
		writer.fwrite("<tr><td>Finish time: <td><span id='finishTime'>Calculating...</span>\n");
	}

	@Override
	protected void reportHeaderTotal(ReportWriter writer, Date date) throws IOException
	{
		writer.fwrite("<tr>");
		writer.fwrite("<td colspan='2'>");
		writer.fwrite("<button class='btn btn-info filterTotal' type='button'>Executed : <span id='exec' class='badge'>0</span></button>");
		writer.fwrite("<button class='btn btn-success filterPassed' type='button'>Passed : <span id='pass' class='badge'>0</span></button>");
		writer.fwrite("<button class='btn btn-danger filterFailed' type='button'>Failed : <span id='fail' class='badge'>0</span></button>");
		writer.fwrite("<button class='btn btn-default filterExpandAllFailed' type='button'><span class='text-danger'>Expand all failed</span></button>");
		writer.fwrite("<button class='btn btn-default filterCollapseAll' type='button'>Collapse all</button>");
		writer.fwrite("<button class='btn btn-default timestamp' type='button'>Time off/on</button>");
		writer.fwrite("</td>");
		writer.fwrite("</tr>");
		writer.fwrite("</table>\n");

		writer.fwrite("<table class='table repLog table-bordered'>\n");
//		writer.fwrite(createColgroup());
		writer.fwrite("<tbody>");
	}

	@Override
	protected void reportFooter(ReportWriter writer, int failed, int passed, Date startTime, Date finishTime, String name, String reportName) throws IOException
	{
		writer.fwrite("</tbody>");
		writer.fwrite("</table>");
		writer.fwrite("<script type='text/javascript'>\n" +
						"<!--\n" +
						"document.getElementById('exec').innerHTML = '<span> %d </span>'\n" +
						"document.getElementById('pass').innerHTML = '<span> %d </span>'\n" +
						"document.getElementById('fail').innerHTML = '<span> %d </span>'\n" +
						"document.getElementById('startTime').innerHTML = '<span>%tF %tT</span>'\n" +
						"document.getElementById('finishTime').innerHTML = '<span>%tF %tT</span>'\n" +
						"document.getElementById('name').innerHTML = '<span>%s</span>'\n" +
						"document.getElementById('reportName').innerHTML = '<span>%s</span>'\n" +
						"-->\n" +
						"</script>\n",
				passed + failed,
				passed,
				failed,
				startTime, startTime, 
				finishTime, finishTime,
				Str.asString(name),
				reportName
		);

		writer.fwrite("</body>\n");
		writer.fwrite("</html>");
	}
	//endregion

	//region display executed matrix
	@Override
	protected void reportMatrixHeader(ReportWriter writer, String matrixName) throws IOException
	{
		writer.fwrite("<tr><td width='200'><a href='#' class='showSource'>Matrix <span class='caret'></span>  </a><td><span id='reportName'>%s</span>\n", matrixName);
		writer.fwrite("<tr class='matrixSource'><td colspan='2'>\n");
		writer.fwrite("<script>\n");
		writer.fwrite("function copyToClipboard(elem) {\n"
				+ "var clone = $('#copy').clone();\n"
				+ "$('#copy').remove();\n"
				+ "    var targetId = '_hiddenCopyText_';\n"
				+ "    var origSelectionStart, origSelectionEnd;\n"
				+ "    target = document.getElementById(targetId);\n"
				+ "    if (!target) {\n"
				+ "var target = document.createElement(\"textarea\");\n"
				+ "target.style.position = 'absolute';\n"
				+ "target.style.left = '-9999px';\n"
				+ "target.style.top = '0';\n"
				+ "target.id = targetId;\n"
				+ "        document.body.appendChild(target);\n"
				+ "        }\n"
				+ "        target.textContent = elem.textContent;\n"
				+ "    \n"
				+ "    var currentFocus = document.activeElement;\n"
				+ "    target.focus();\n"
				+ "    target.setSelectionRange(0, target.value.length);\n"
				+ "    var succeed;\n"
				+ "    try {\n"
				+ "     succeed = document.execCommand(\"copy\");\n"
				+ "    } catch(e) {\n"
				+ "        succeed = false;\n"
				+ "    }\n"
				+ "    if (currentFocus && typeof currentFocus.focus === \"function\") {\n"
				+ "        currentFocus.focus();\n"
				+ "    }\n"
				+ "    target.textContent = \"\";\n"
				+ "    $('pre').prepend(clone); \n"
				+ "    return succeed;\n"
				+ "  }");
		writer.fwrite("</script>\n");
		writer.fwrite("<pre id='matrixSource'>");
		writer.fwrite("<button id=\"copy\" onclick=\"copyToClipboard(document.getElementById('matrixSource'))\" class='btn btn-default copyMatrix'>Copy</button>\n");
	}

	@Override
	protected void reportMatrixFooter(ReportWriter writer) throws IOException
	{
		writer.fwrite("</pre>\n");
	}

	@Override
	protected void reportMatrixRow(ReportWriter writer, int count, String line) throws IOException
	{
		writer.fwrite("%s\n", line);
	}

	//endregion

	@Override
	protected void reportItemHeader(ReportWriter writer, MatrixItem item, Integer id) throws IOException
	{
        String itemId = item.getId();

        if (itemId == null)
		{
            itemId = "";
        }

		//region display header

		String collect = item.getComments().stream().map(CommentString::toString).collect(Collectors.joining("<br>\n"));
		if (!collect.isEmpty())
		{
			writer.fwrite(
					"<tr class='comment'>\n"+
						"<td colspan='%s'>\n" +
							"%s"+
						"</td>\n" +
					"</tr>\n"
					,this.columnCount, collect
			);
		}
		writer.fwrite("<tr id='tr_%s'>", id);
		writer.fwrite("<td class='timestamp'>%s</td>", DateTime.current().str());
		writer.fwrite("<th scope='row'>%03d</th>", item.getNumber());
		writer.fwrite("<td>%s</td>", itemId);
		writer.fwrite("<td><a href='javascript:void(0)' class='showBody'>%s</a></td>", item.getItemName());
		writer.fwrite("<td id='hs_%s'> </td>", id);
		writer.fwrite("<td id='time_%s'> </td>", id);
		writer.fwrite("<td id='scr_%s'> </td>", id);
		writer.fwrite("</tr>");
		//endregion

		writer.fwrite("<tr>");
		writer.fwrite("<td colspan='%s' class='parTd'>", this.columnCount);
		writer.fwrite("<table class='table table-bordered innerTable'>");
//		writer.fwrite(createColgroup());
		writer.fwrite("<tbody>");
	}

	private String createColgroup()
	{
		return "<colgroup>\n" +
				"  <col width='5%'>\n" +
				"  <col width='10%'>\n" +
				"  <col width='40%'>\n" +
				"  <col width='15%'>\n" +
				"  <col width='15%'>\n" +
				"  <col width='15%'>\n" +
				"</colgroup>\n";
	}

	@Override
	protected void reportItemFooter(ReportWriter writer, MatrixItem item, Integer id, long time, ImageWrapper screenshot) throws IOException
	{
		Result result = item.getResult() == null ? Result.NotExecuted : item.getResult().getResult();
		String styleClass = result.isFail() ? "danger" : "success";
		writer.fwrite("</tbody>");
		writer.fwrite("</table>");

		//region javascript insert
		writer.fwrite("<script type='text/javascript'>\n");
		writer.fwrite("$('#tr_%s').addClass('%s');\n", id, styleClass);
		writer.fwrite("$('#hs_%s').html('<strong class=\"text-%s\">%s</strong>');\n", id, styleClass, result);
		writer.fwrite("$('#time_%s').html('%s ms');\n", id, time <= 1 ? "< 1" : time);
		if (screenshot != null)
		{
			String link = decorateLink(screenshot.getDescription(), getImageDir() + "/" + screenshot.getName(getReportDir()));
			writer.fwrite("$('#scr_%s').html('%s');\n",id,link);
		}
		writer.fwrite("</script>\n");
		//endregion

		writer.fwrite("</td>");
		writer.fwrite("</tr>");
	}

	@Override
	protected void reportImage(ReportWriter writer, MatrixItem item, String beforeTestcase, String fileName, String embedded, String title, int scale, ImageReportMode reportMode) throws IOException
	{
		if (beforeTestcase != null)
		{
			writer.fwrite(
					"<div class='movable' data-moveto='%s' >\n",
					beforeTestcase);
		}
		writer.fwrite(
				"<span class='tableTitle'>%s</span><br>",
				this.postProcess(title));

		switch (reportMode)
        {
        case AsEmbeddedImage:
            writer.fwrite("<img src='data:image/jpeg;base64,%s' class='img'/><br>", embedded);
            break;

        case AsImage:
            writer.fwrite("<img src='%s' class='img'/><br>", fileName);
            break;

        case AsLink:
            writer.fwrite("<a href=" + fileName+ ">Image</a><br>");
            break;

        default:
            break;
        }

		if (beforeTestcase != null)
		{
			writer.fwrite("</div>\n");
		}
	}

    @Override
    protected void reportContent(ReportWriter writer, MatrixItem item, String beforeTestcase, Content content,
            String title) throws IOException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
	protected void reportItemLine(ReportWriter writer, MatrixItem item, String beforeTestcase, String string, String labelId) throws IOException
	{
		if (labelId == null)
		{
			writer.fwrite(postProcess(string));
		}
		else
		{
			writer.fwrite(
					  "<tr>\n"
					+ "  <td colspan='%s'>\n"
					+ "     <span class='label' id='%s'>%s</span>\n"
					+ "  </td>\n"
					+ "</tr>\n"
					, this.columnCount, labelId, string);
		}
	}

	@Override
	protected void tableHeader(ReportWriter writer, ReportTable table, String tableTitle, String[] columns, int[] percents) throws IOException
	{
		writer.fwrite("<tr>\n");
		writer.fwrite("<td>\n");
		if (table.getBeforeTestcase() != null)
		{
			writer.fwrite("<div class='movable' data-moveto='%s' >\n",table.getBeforeTestcase());
		}
        writer.fwrite("<span>%s</span>", this.postProcess(tableTitle));
        if (table.isBordered())
        {
            writer.fwrite("<table width='100%%' class='table table-bordered'>\n");
        }
        else
        {
            writer.fwrite("<table width='100%%' class='table'>\n");
        }

		//region display headers
		writer.fwrite("<thead>\n");
        for (int i = 0; i < columns.length; i++)
        {
            int percent = (percents == null || percents.length <= i) ? 0 : percents[i]; 
        	if (percent <= 0)
        	{
        		writer.fwrite("<th>%s</th>", columns[i]);
        	}
        	else
        	{
        		writer.fwrite("<th width='%d%%'>%s</th>", percent, columns[i]);
        	}
        }
        writer.fwrite("</thead>\n");
		//endregion

		writer.fwrite("<tbody>\n");

	}
	
	@Override
	protected void tableRow(ReportWriter writer, ReportTable table, int quotes, Object ... value) throws IOException
	{
		if (value != null)
        {
			writer.fwrite("<tr>");
			int count = 0;
			for (Object obj : value)
			{
				String string = ReportHelper.objToString(obj, count >= quotes);
				String[] split = string.split("\n");
				StringBuilder sb = new StringBuilder();
				for (String str : split)
				{
					str = str.replace("\t","&nbsp;&nbsp;&nbsp;&nbsp;");
					sb.append(str).append("<br>");
				}
				writer.fwrite("<td class='tdMax'>%s</td>", sb.toString());
				count++;
			}
			writer.fwrite("</tr>");
            writer.fwrite("\n");
        }
	}

	@Override
	protected void tableFooter(ReportWriter writer, ReportTable table) throws IOException
	{
        writer.fwrite("</tbody>\n");
		writer.fwrite("</table>\n");
		if (table.getBeforeTestcase() != null)
		{
			writer.fwrite("</div>\n");
		}
		writer.fwrite("</td>");
		writer.fwrite("</tr>");
	}

	@Override
	protected void reportChart(ReportWriter writer, String title, String beforeTestCase, ChartBuilder chartBuilder) throws IOException
	{
		if (beforeTestCase != null)
		{
			writer.fwrite(
					"<div class='movable' data-moveto='%s' >\n",
					beforeTestCase);
		}
		writer.fwrite(
				"<span class='tableTitle'>%s</span><br>",
				this.postProcess(title));

		chartBuilder.report(writer, ++chartCount);

		if (beforeTestCase != null)
		{
			writer.fwrite("</div>\n");
		}
		
	}
}
