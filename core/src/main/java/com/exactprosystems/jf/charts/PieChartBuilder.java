/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.jf.charts;

import com.exactprosystems.jf.actions.ReadableValue;
import com.exactprosystems.jf.api.error.JFException;
import com.exactprosystems.jf.common.report.ReportWriter;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.functions.Table;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PieChartBuilder extends ChartBuilder
{
	public PieChartBuilder() throws JFException
	{
		super(null, null, null);
	}

	public PieChartBuilder(Table table, Parameters params, Map<String, Color> colors) throws JFException
	{
		super(table, params, colors);
	}

	@Override
	public void report(ReportWriter writer, Integer id) throws IOException
	{
//		AtomicBoolean ab = new AtomicBoolean(false);
//		int headerSize = table.getHeaderSize();
//		IntStream.range(0, headerSize)
//				.mapToObj(table::getHeader)
//				.filter(valueColumnName::equals)
//				.findFirst()
//				.ifPresent(s1 -> ab.set(true));
//
//		if (!ab.get())
//		{
//			throw new ChartException(String.format("Column with name %s is not presented", valueColumnName));
//		}
//		if (!table.stream().allMatch(rt -> isNumber(rt.get(valueColumnName).toString())))
//		{
//			throw new ChartException(String.format("All values from column %s must be a number", valueColumnName));
//		}

//		if (labelColumn == null)
//		{
//			this.labelColumnName = IntStream.range(0, headerSize)
//					.mapToObj(table::getHeader)
//					.filter(s -> !valueColumnName.equals(s))
//					.findFirst()
//					.orElseThrow(() -> new ChartException("Pie chart can't be drawing from table with one column"));
//		}
//		else
//		{

//		}

		String chartId = "chart_" + id;
		writer.fwrite("<div id='%s' class=container></div>", chartId);
		String data = createData();
		String colors = createColors();
		writer.fwrite("<script>createPieChart('%s',%s, %s)</script>", chartId, data, colors);
	}

	private String createData()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String separator = "";
		for (int i = 0; i < this.table.getHeaderSize(); i++)
		{
			String label = this.table.getHeader(i);
			String value = String.valueOf(this.table.get(0).get(label));
			sb.append(separator).append(String.format("{'value' : %s, 'label' : '%s'}", value, label));
			separator = ",";
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public void helpToAddParameters(List<ReadableValue> list, Context context) throws Exception
	{
	}
}