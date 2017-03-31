////////////////////////////////////////////////////////////////////////////////
//Copyright (c) 2009-2015, Exactpro Systems, LLC
//Quality Assurance & Related Development for Innovative Trading Systems.
//All rights reserved.
//This is unpublished, licensed software, confidential and proprietary
//information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.functions;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.exactprosystems.jf.api.app.ImageWrapper;
import com.exactprosystems.jf.api.app.Mutable;
import com.exactprosystems.jf.api.common.Converter;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.conditions.Condition;
import com.exactprosystems.jf.common.ChangeListener;
import com.exactprosystems.jf.common.evaluator.AbstractEvaluator;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportHelper;
import com.exactprosystems.jf.common.report.ReportTable;
import com.exactprosystems.jf.common.undoredo.Command;
import com.exactprosystems.jf.documents.matrix.parser.Parameters;
import com.exactprosystems.jf.exceptions.ColumnIsPresentException;
import com.exactprosystems.jf.sql.SqlConnection;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Table implements List<RowTable>, Mutable, Cloneable
{
	//region fields
	private static final String EMPTY_HEADER	= "newH";
	private static final String EMPTY_ROW		= "newR";

	private BiConsumer<Command, Command> undoRedoFunction;
	private Consumer<Table> displayFunction;

	private boolean useColumnNumber = false;
	private static final String ROW_INDEX_SYMBOL = "@";
	private List<Map<Header, Object>> innerList = null;
	private Header[] headers;
	private AbstractEvaluator evaluator;

	private ChangeListener changeListener;

	private String fileName;
	static int index = 0;
	private boolean changed;
	private static final Logger logger = Logger.getLogger(Table.class);
	//endregion

	//region Constructors
	private Table(AbstractEvaluator evaluator)
	{
		this.evaluator = evaluator;
		this.innerList = new ArrayList<>();
	}

	public Table(Table table, AbstractEvaluator evaluator)
	{
		this(evaluator);
		this.addColumns(Arrays.stream(table.headers).map(h -> h.name).toArray(String[]::new));
		for (int i = 0; i < table.innerList.size(); i++)
		{
			Map<String, Object> stringObjectMap = convertToStr(table.innerList.get(i));
			Map<Header, Object> newMap = new LinkedHashMap<>();
			stringObjectMap.entrySet().forEach(entry -> newMap.put(headerByName(entry.getKey()), entry.getValue()));
			this.innerList.add(newMap);
		}
	}

	public Table(String[] headers, AbstractEvaluator evaluator)
	{
		this(evaluator);
		addColumns(headers);
	}

	public Table(String[][] lines, AbstractEvaluator evaluator)
	{
		this(evaluator);

		String[] firstLine = lines[0];
		addColumns(firstLine);
		for (int i = 1; i < lines.length; i++)
		{
			String[] line = lines[i];
			
			RowTable res = new RowTable();
			
			for (int j = 0; j < line.length; j++)
			{
				res.put(firstLine[j], line[j]);
			}
			this.add(res);
		}
	}

	public Table(ResultSet set, AbstractEvaluator evaluator) throws Exception
	{
		this(evaluator);

		try
		{
			ResultSetMetaData meta = set.getMetaData();
			this.headers = new Header[meta.getColumnCount()];

			for (int column = 0; column < meta.getColumnCount(); column++)
			{
				this.headers[column] = new Header(meta.getColumnLabel(column + 1), Header.HeaderType.forName(meta.getColumnClassName(column + 1)));
			}

			while (set.next())
			{
				Map<Header, Object> line = new LinkedHashMap<>();

				for (int i = 0; i < headers.length; i++)
				{
				    int type = set.getMetaData().getColumnType(i + 1);
				    
				    Object value = null;
					if (type == Types.LONGVARBINARY || type == Types.BLOB || type == Types.VARBINARY)
				    {
				        value = set.getBlob(i + 1);
	                    value = Converter.blobToStorable((Blob)value);
				    }
				    else
				    {
				        value = set.getObject(i + 1);
				    }
				    
					line.put(headers[i], value);
				}

				this.innerList.add(line);
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public Table(String fileName, char delimiter, AbstractEvaluator evaluator) throws Exception
	{
		this(evaluator);

		this.fileName = fileName;
		
		try (Reader reader = new BufferedReader(new FileReader(fileName)))
		{
			read(reader, delimiter);
		}
	}

	public Table(Reader reader, char delimiter, AbstractEvaluator evaluator) throws Exception
	{
		this(evaluator);
		read(reader, delimiter);
	}

	public Table(String dirName, AbstractEvaluator evaluator) throws Exception
	{
		this(evaluator);
		readFilesInfo(dirName);
	}
	//endregion

	//region Static constructors
	public static Table emptyTable()
	{
		return new Table(new String[][]{new String[]{EMPTY_HEADER}, new String[]{EMPTY_ROW}}, null);
	}
	//endregion

	//region Interface Mutable
	@Override
	public boolean isChanged()
	{
		return this.changed;
	}

	@Override
	public void saved()
	{
		this.changed = false;
	}
	//endregion

	//region Interface Cloneable
	@Override
	public Table clone() throws CloneNotSupportedException
	{
		Table clone = (Table)super.clone();
		
		clone.fileName = this.fileName;
		clone.headers = this.headers.clone();
		clone.innerList = new ArrayList<>();
		for (Map<Header, Object> item : this.innerList)
		{
			Map<Header, Object> copy = new LinkedHashMap<>();
			for (Entry<Header, Object> e : item.entrySet())
			{
				copy.put(e.getKey(), e.getValue());
			}
			clone.innerList.add(copy);
		}
		
		return clone;
	}
	//endregion

	public static boolean extendEquals(ReportBuilder report, Table differences, Table actual, Table expected, String[] exclude, boolean ignoreRowsOrder)
	{
		Set<String> expectedNames = expected.names(exclude);
		Set<String> actualNames   = actual.names(exclude);
		ReportTable table = null;

		if (!expectedNames.equals(actualNames))
		{
			table = addMismatchedRow(table, report, differences, "", Arrays.toString(expectedNames.toArray()), Arrays.toString(actualNames.toArray()));
			return false;
		}

		boolean result = true;
		if (ignoreRowsOrder)
		{
			boolean[] actualMatched = new boolean[actual.innerList.size()]; 
			boolean[] expectedMatched = new boolean[expected.innerList.size()];

			int actualCounter = 0;
			Iterator<Map<Header, Object>> actualIterator = actual.innerList.iterator();
			while (actualIterator.hasNext())
			{
				Map<Header, Object> actualRow = actualIterator.next();

				int expectedCounter = 0;
				Iterator<Map<Header, Object>> expectedIterator = expected.innerList.iterator();
				while (expectedIterator.hasNext())
				{
					Map<Header, Object> expectedRow = expectedIterator.next();
					Map<String, ArrayList<Object>> stringMap = compareRows(actual.convertToStr(actualRow), expected.convertToStr(expectedRow), expectedNames);
					if (stringMap.isEmpty())
					{
						actualMatched[actualCounter] = true;
						expectedMatched[expectedCounter] = true;
					}

					expectedCounter++;
				}
				actualCounter++;
			}

			int count = 0;
			Iterator<Map<Header, Object>> expectedIterator = expected.innerList.iterator();
			while (expectedIterator.hasNext())
			{
				Map<Header, Object> expectedRow = expectedIterator.next();
				if (!expectedMatched[count])
				{
					table = addMismatchedRow(table, report, differences, "" + count, ReportHelper.objToString(expectedRow, false), "");
					result = false;
				}
				count++;
			}

			count = 0;
			actualIterator = actual.innerList.iterator();
			while (actualIterator.hasNext())
			{
				Map<Header, Object> actualRow = actualIterator.next();
				if (!actualMatched[count])
				{
					table = addMismatchedRow(table, report, differences, "" + count, "", ReportHelper.objToString(actualRow, false));
					result = false;
				}
				count++;
			}
		}
		else
		{
			Iterator<Map<Header, Object>> actualIterator   = actual.innerList.iterator();
			Iterator<Map<Header, Object>> expectedIterator = expected.innerList.iterator();

			int rowCount = 0;
			while (actualIterator.hasNext() && expectedIterator.hasNext())
			{
				Map<Header, Object> actualRow = actualIterator.next();
				Map<Header, Object> expectedRow = expectedIterator.next();
				Map<String, ArrayList<Object>> stringMap = compareRows(actual.convertToStr(actualRow), expected.convertToStr(expectedRow), expectedNames);
				if (!stringMap.isEmpty())
				{
					table = addMismatchedRow(table, report, differences, "" + rowCount, ReportHelper.objToString(expectedRow, false), ReportHelper.objToString(actualRow, false));
					for (Entry<String, ArrayList<Object>> entry : stringMap.entrySet())
					{
	                    table = addMismatchedRow(table, report, differences, entry.getKey(), "" + entry.getValue().get(0), "" + entry.getValue().get(1));
					}
					result = false;
				}

				rowCount++;
			}
			while (expectedIterator.hasNext())
			{
				Map<Header, Object> expectedRow = expectedIterator.next();
				table = addMismatchedRow(table, report, differences, "" + rowCount, ReportHelper.objToString(expectedRow, false), "");
				rowCount++;
				result = false;
			}
			while (actualIterator.hasNext())
			{
				Map<Header, Object> actualRow = actualIterator.next();
				table = addMismatchedRow(table, report, differences, "" + rowCount, "", ReportHelper.objToString(actualRow, false));
				rowCount++;
				result = false;
			}
		}

		return result;
	}

	public static String generateColumnName(Table table)
	{
		int currentIndexColumn = 0;
		String columnName = "NewColumn", temp = "NewColumn";
		while (table.columnIsPresent(columnName))
		{
			columnName = temp + currentIndexColumn++;
		}
		return columnName;
	}

	@Override
	public String toString()
	{
		return Table.class.getSimpleName() + " [" + this.fileName + ":" + Arrays.toString(this.headers) + ":" + size() + "]";
	}

	//==============================================================================================
	// Interface List
	//==============================================================================================

	@Override
	public int size()
	{
		return this.innerList.size();
	}

	@Override
	public boolean isEmpty()
	{
		return this.innerList.isEmpty();
	}

	@Override
	public boolean contains(Object o)
	{
		return this.innerList.contains(o);
	}

	@Override
	public Iterator<RowTable> iterator()
	{
		return new Iterator<RowTable>()
		{
			private Iterator<Map<Header, Object>> iterator = innerList.iterator();

			@Override
			public boolean hasNext()
			{
				return this.iterator.hasNext();
			}

			@Override
			public RowTable next()
			{
				return convertToStr(this.iterator.next());
			}

			@Override
			public void remove()
			{
				innerList.iterator().remove();
			}
		};
	}

	@Override
	public Object[] toArray()
	{
		return this.innerList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		return this.innerList.toArray(a);
	}

	@Override
	public boolean add(RowTable e)
	{
		changed(true);
		return this.innerList.add(convert(e));
	}

	@Override
	public boolean remove(Object o)
	{
		changed(true);
		return this.innerList.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		return this.innerList.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends RowTable> c)
	{
		changed(true);
		return this.innerList.addAll(convert(c));
	}

	@Override
	public boolean addAll(int index, Collection<? extends RowTable> c)
	{
		changed(true);
		return this.innerList.addAll(index, convert(c));
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		changed(true);
		return this.innerList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		changed(true);
		return this.innerList.retainAll(c);
	}

	@Override
	public void clear()
	{
		changed(true);
		this.innerList.clear();
	}

	@Override
	public RowTable get(int index)
	{
		return convertToStr(this.innerList.get(index));
	}

	@Override
	public RowTable set(int index, RowTable element)
	{
		changed(true);
		Map<Header, Object> convert = convert(element);
		Map<Header, Object> set = this.innerList.set(index, convert);
		return convertToStr(set);
	}

	@Override
	public void add(int index, RowTable element)
	{
		changed(true);
		this.innerList.add(index, convert(element));
	}

	private void changed(boolean flag)
	{
		this.changed = flag;
		Optional.ofNullable(this.changeListener).ifPresent(c -> c.change(this.changed));
	}

	@Override
	public RowTable remove(int index)
	{
		changed(true);
		Map<Header, Object> remove = this.innerList.remove(index);
		return convertToStr(remove);
	}

	@Override
	public int indexOf(Object o)
	{
		return this.innerList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o)
	{
		return this.innerList.lastIndexOf(o);
	}

	@Override
	public ListIterator<RowTable> listIterator()
	{
		return new TableListIterator(innerList.listIterator());
	}

	@Override
	public ListIterator<RowTable> listIterator(int index)
	{
		return new TableListIterator(innerList.listIterator(index));
	}

	@Override
	public List<RowTable> subList(int fromIndex, int toIndex)
	{
		List<Map<Header, Object>> maps = this.innerList.subList(fromIndex, toIndex);
		List<RowTable> res = new ArrayList<>();
		for (Map<Header, Object> map : maps)
		{
			res.add(convertToStr(map));
		}
		return res;
	}
	//endregion

	public Table sort(String colName, boolean az) throws Exception
	{
		changed(true);
		for (int i = 0; i < this.headers.length; i++)
		{
			if (this.headers[i].name.equals(colName))
			{
				return this.sort(i, az);
			}
		}
		throw new Exception("Column this name \'" + colName + "\' not fount in the table");
	}

	public Table sort(int colNumber, boolean az)
	{
		changed(true);
		Header header = this.headers[colNumber];
		this.innerList.sort((o1, o2) ->
		{
			Object obj1 = o1.get(header);
			Object obj2 = o2.get(header);

			obj1 = convertCell(header, obj1, null);
			obj2 = convertCell(header, obj2, null);

			Header.HeaderType type = header.type == null ? Header.HeaderType.STRING : header.type;
			int compare = type.compare(obj1, obj2);
			return az ? compare : -compare;
		});
		return this;
	}

	public void considerAsString(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.STRING, columns);
	}

	public void considerAsBoolean(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.BOOL, columns);
	}

	public void considerAsInt(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.INT, columns);
	}

	public void considerAsDouble(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.DOUBLE, columns);
	}

	public void considerAsDate(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.DATE, columns);
	}

	public void considerAsBigDecimal(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.BIG_DECIMAL, columns);
	}

	public void considerAsExpression(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.EXPRESSION, columns);
	}

	public void considerAsGroup(String... columns) throws Exception
	{
		considerAs(Header.HeaderType.GROUP, columns);
	}

	public Table select(Condition[] conditions)
	{
		Table result = new Table(this.evaluator);
		result.headers = this.headers.clone();

		for (Map<Header, Object> row : this.innerList)
		{
			RowTable rowTable =  convertToStr(row);
			boolean matched = true;
			for (Condition condition : conditions)
			{
				if (!condition.isMatched(rowTable))
				{
					matched = false;
					break;
				}
			}

			if (matched)
			{
				result.innerList.add(row);
			}
		}

		return result;
	}

	public void upload(SqlConnection connection, String table) throws SQLException
	{
		Statement statement = null;
		try
		{
			statement = connection.getConnection().createStatement();
			Header[] headers = this.headers;
			int i = 0;
			for (Map<Header, Object> map : this.innerList)
			{
				StringBuilder sql = new StringBuilder("INSERT INTO ");
				sql.append(table).append(" SET");

				for (Header header : headers)
				{
					sql.append(" ").append(header.name).append("=");
					sql.append("'").append(map.get(header)).append("',");
				}
				sql.deleteCharAt(sql.length() - 1);
				statement.addBatch(sql.toString());
				if (i == 10)
				{
					statement.executeBatch();
					i = 0;
				}
				else
				{
					i++;
				}
			}
		}
		finally
		{
			if (statement != null)
			{
				statement.executeBatch();
				statement.close();
			}
		}
	}

	public List<Integer> findAllIndexes(Condition[] conditions)
	{
		List<Integer> indexes = new ArrayList<>();
		int count = 0;
		for (Map<Header, Object> row : this.innerList)
		{
			RowTable rowTable =  convertToStr(row);
			boolean matched = true;
			for (Condition condition : conditions)
			{
				if (!condition.isMatched(rowTable))
				{
					matched = false;
					break;
				}
			}

			if (matched)
			{
				indexes.add(count);
			}
			count++;
		}

		return indexes;
	}

	public void replace(Object source, Object dest, boolean matchCell, String ...columns)
	{
		if (columns == null || columns.length == 0 || areEqual(source, dest))
		{
			return;
		}

		List<Header> filtered = filter(columns);

		for (Map<Header, Object> row : this.innerList)
		{
			for (Header header : filtered)
			{
				Object value = row.get(header);
				if (!matchCell)
				{
					String sValue = String.valueOf(value);
					String sSource = String.valueOf(source);
					if (sValue.contains(sSource)) // TODO why contains ?!!
					{
						row.remove(header);
						row.put(header, sValue.replace(sSource, String.valueOf(dest)));
					}
				}
				else if (areEqual(value, source))
				{
					row.put(header, dest);
				}
			}
		}
	}

	public void replace(String regexp, Object dest, String ...columns)
	{
		if (columns == null || columns.length == 0)
		{
			return;
		}

		List<Header> filtered = filter(columns);

		for (Map<Header, Object> row : this.innerList)
		{
			for (Header header : filtered)
			{
				Object value = row.get(header);
				if (String.valueOf(value).matches(regexp))
				{
					row.put(header, dest);
				}
			}
		}
	}

	public void report(ReportBuilder report, String title, String beforeTestcase, boolean withNumbers, boolean reportValues) throws Exception
	{
		String[] columns = Arrays.stream(this.headers).map(h -> h.name).toArray(num -> new String[num]);

		report(report, title, beforeTestcase, withNumbers, reportValues, null, columns);
	}

	public void report(ReportBuilder report, String title, String beforeTestcase, boolean withNumbers,
					   boolean reportValues, Parameters newColumns, String... columns) throws Exception
	{
		if (beforeTestcase != null || report.reportIsOn()) // TODO zzz
		{
			int[] columnsIndexes = getIndexes(columns);

			if (columnsIndexes.length == 0)
			{
				columnsIndexes = new int[this.headers.length];
				for (int i = 0; i < this.headers.length; i++)
				{
					columnsIndexes[i] = i;
				}
			}

			int addition = withNumbers ? 1 : 0;
			String[] headers = new String[addition + columnsIndexes.length];
			if (withNumbers)
			{
				headers[0] = "#";
			}
			int col = 0;
			for (int index : columnsIndexes)
			{
				headers[col++ + addition] = this.headers[index].name;
			}
			headers = convertHeaders(newColumns, headers, withNumbers);
			ReportTable table = report.addExplicitTable(title, beforeTestcase, true, 0, new int[] {}, headers);

			Function<String, String> func = name -> newColumns == null ? name
					: newColumns.entrySet().stream().filter(e -> name.equals(String.valueOf(e.getValue()))).findFirst()
					.map(Entry::getKey).orElse(name);

			int count = 0;
			for (Map<Header, Object> row : this.innerList)
			{
				Object[] value = new Object[headers.length];
				if (withNumbers)
				{
					value[0] = count;
				}

				for (int i = addition; i < headers.length; i++)
				{
					Header header = headerByName(func.apply(headers[i]));
					if (reportValues)
					{
						value[i] = convertCell(header, row.get(header), report);
					}
					else
					{
						Object v = row.get(header);
						if (v instanceof ImageWrapper)
						{
							ImageWrapper iw = (ImageWrapper) v;
							String description = iw.getDescription() == null ? iw.toString() : iw.getDescription();
							v = report.decorateLink(description,
									report.getImageDir() + File.separator + iw.getName(report.getReportDir()));
						}
						else if (v instanceof ReportBuilder)
						{
							ReportBuilder rb = (ReportBuilder) v;
							String name = rb.getName();

							v = report.decorateLink(name, name);
						}
						value[i] = v;
					}
				}
				table.addValues(value);
				count++;
			}
		}
	}

	public boolean removeRow(int index)
	{
		this.innerList.remove(index);
		changed(true);
		return true;
	}

	public boolean isEmptyTable()
	{
		return this.headers.length == 1
				&& this.headers[0].name.equals(EMPTY_HEADER)
				&& this.innerList.size() == 1
				&& this.innerList.get(0).get(headerByName(EMPTY_HEADER)).equals(EMPTY_ROW);
	}

	public void fillFromTable(Table table)
	{
		this.headers = new ArrayList<Header>().toArray(new Header[0]);
		this.innerList.clear();
		this.addColumns(Arrays.stream(table.headers).map(h -> h.name).toArray(String[]::new));
		for (int i = 0; i < table.innerList.size(); i++)
		{
			Map<String, Object> stringObjectMap = convertToStr(table.innerList.get(i));
			Map<Header, Object> newMap = new LinkedHashMap<>();
			stringObjectMap.entrySet().forEach(entry -> newMap.put(headerByName(entry.getKey()), entry.getValue()));
			this.innerList.add(newMap);
		}
	}
	
	public void setEvaluator(AbstractEvaluator evaluator)
	{
		this.evaluator = evaluator;
	}
	
	public boolean save(String fileName, char delimiter, boolean saveValues, boolean withNmumbers)
	{
		CsvWriter writer = null;

		try (Writer bufferedWriter = new BufferedWriter(new FileWriter(fileName)))
		{
			writer = new CsvWriter(bufferedWriter, delimiter);
			return save(writer, "", saveValues, withNmumbers);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			return false;
		}
		finally
		{
			if (writer != null)
			{
				writer.close();
			}
		}
	}
	
	public boolean save(CsvWriter writer, String indent, boolean saveValues, boolean withNmumbers) throws IOException
	{
		int columns = this.headers.length + (withNmumbers ? 1 : 0);
		String[] record = new String[columns];
		int count = 0;
		if (withNmumbers)
		{
			record[count++] = indent + ROW_INDEX_SYMBOL;
		}
		for (int i = 0; i < this.headers.length; i++)
		{
			record[count++] = this.headers[i].name;
		}
		writer.writeRecord(record, true);


		List<Map<Header, Object>> innerList1 = this.innerList;
		for (int j = 0; j < innerList1.size(); j++)
		{
			count = 0;
			if (withNmumbers)
			{
				record[count++] = indent + String.valueOf(j);
			}
			Map<Header, Object> f = innerList1.get(j);
			for (int i = 0; i < this.headers.length; i++)
			{
				Object source = f.get(this.headers[i]);
				Object value = null;
				if (saveValues)
				{
					value = convertCell(headers[i], source, null);
				}
				else
				{
					value = source;
				}
				record[count++] = String.valueOf(value == null ? "" : value);
			}
			writer.writeRecord(record, true);
		}
		return true;
	}

	public void addColumns(String... columns)
	{
		changed(true);
		List<Header> list = new ArrayList<Header>();
		if (this.headers != null)
		{
			list.addAll(Arrays.asList(this.headers));
		}
		
		for (String column : columns)
		{
			if (column.equals(ROW_INDEX_SYMBOL))
			{
				this.useColumnNumber = true;
				continue;
			}
			if (!columnIsPresent(column))
			{
				list.add(new Header(column, null));
			}
		}
		
		this.headers = list.toArray(new Header[0]);
		addEmptyStringToAllLinesInNewColumn();
	}

	public boolean columnIsPresent(String columnName)
	{
		if (this.headers == null || this.headers.length == 0)
		{
			return false;
		}
		for (Header header : this.headers)
		{
			if (header != null && Str.areEqual(columnName, header.name))
			{
				return true;
			}
		}
		return false;
	}

	public void updateValue(int index, RowTable row)
	{
		changed(true);
		Map<Header, Object> newMap = new LinkedHashMap<>();
		Map<Header, Object> oldMap = this.innerList.get(index);
		Arrays.stream(this.headers).forEach(h ->
		{
			Object rowValue = row.get(h.name);
			if (rowValue == null)
			{
				rowValue = oldMap.get(h);
			}
			newMap.put(h, rowValue);
		});
		this.innerList.set(index, newMap);
	}

	public void setValue(int index, RowTable row)
	{
		changed(true);
		this.innerList.set(index, convert(row));
	}

	public void setValue(int index, Map<String, Object> map)
	{
		changed(true);
		Map<Header, Object> line = this.innerList.get(index);
		for (Entry<String, Object> entry : map.entrySet())
		{
			String name = entry.getKey();
			Object value = entry.getValue();

			line.put(headerByName(name), value);
		}
	}

	public void renameColumn(String oldValue, String newValue) throws Exception
	{
		for (Header h : this.headers)
		{
			if (h.name.equals(oldValue))
			{
				h.name = newValue;
				return;
			}
		}
		throw new Exception(String.format("Column with name %s not presented into table", oldValue));
	}

	public void addValue(int index, Map<String, Object> map)
	{
		changed(true);
		if (this.headers != null)
		{
			Map<Header, Object> line = convert(map);
			if (index >= 0)
			{
				this.innerList.add(index, line);
			}
			else
			{
				this.innerList.add(line);
			}
		}
	}

	public void addValue(Object[] arr)
	{
		changed(true);
		if (this.headers != null)
		{
			if (this.useColumnNumber)
			{
				arr = Arrays.copyOfRange(arr, 1, arr.length);
			}

			Map<Header, Object> line = convert(arr);
			this.innerList.add(line);
		}
	}

	public int getHeaderSize()
	{
		return this.headers.length;
	}

	public String getHeader(int index)
	{
		return this.headers[index].name;
	}




	//region Undo/Redo functionality
	public void addColumns(int index, String... columns)
	{
		Arrays.stream(columns)
				.filter(this::columnIsPresent)
				.findFirst()
				.ifPresent(column -> {
					throw new ColumnIsPresentException(String.format("Column with name %s already present", column));
				});

		if (this.headers == null)
		{
			this.headers = new Header[]{};
		}
		List<Header> oldHeaders = new ArrayList<>(Arrays.asList(this.headers));
		List<Header> newHeaders = new ArrayList<>(Arrays.asList(this.headers));

		Command undo = () ->
		{
			this.headers = oldHeaders.toArray(new Header[oldHeaders.size()]);
			this.innerList.forEach(map ->
					map.keySet().stream()
							.filter(head -> !oldHeaders.contains(head))
							.forEach(map::remove));
			displayFunction();
		};
		Command redo = () ->
		{
			ArrayList<Header> headers = new ArrayList<>(newHeaders);
			headers.addAll(index, Arrays.stream(columns).map(s -> new Header(s, null)).collect(Collectors.toList()));
			this.headers = headers.toArray(new Header[headers.size()]);
			addEmptyStringToAllLinesInNewColumn();
			displayFunction();
		};
		changed(true);
		addUndoRedo(undo, redo);
	}

	public void removeColumns(String... columns)
	{
		if (this.headers == null)
		{
			return;
		}

		List<Header> oldHeaders = new ArrayList<>(Arrays.asList(this.headers));
		List<Header> newHeaders = new ArrayList<>(Arrays.asList(this.headers));
		Map<Integer, Map<Header, Object>> removedValues = new LinkedHashMap<>();

		List<String> removedColumnsList = Arrays.asList(columns);

		Iterator<Header> newHeadersIterator = newHeaders.iterator();
		while (newHeadersIterator.hasNext())
		{
			Header header = newHeadersIterator.next();
			String headerName = header.name;
			if (removedColumnsList.contains(headerName))
			{
				//save all removed values to temp map
				IntStream.range(0, this.innerList.size())
						.forEach(i -> {
							Map<Header, Object> map = removedValues.computeIfAbsent(i, k -> new LinkedHashMap<>());
							map.put(header, this.innerList.get(i).get(header));
						});
				//remove header
				newHeadersIterator.remove();
			}
		}

		Command undo = () ->
		{
			this.headers = oldHeaders.toArray(new Header[oldHeaders.size()]);
			//restore all removed values back
			removedValues.forEach((i,map) -> this.innerList.get(i).putAll(map));
			displayFunction();
		};
		Command redo = () ->
		{
			this.headers = newHeaders.toArray(new Header[newHeaders.size()]);
			removedValues.values()
					.stream()
					.map(Map::keySet)
					.flatMap(Collection::stream)
					.distinct()
					.forEach(header -> this.innerList.forEach(row -> row.remove(header)));
			displayFunction();
		};
		changed(true);
		addUndoRedo(undo, redo);
	}

	public void addValue(int index, Object[] arr)
	{
		if (this.headers == null)
		{
			return;
		}

		Command undo = () ->
		{
			this.innerList.remove(index);
			displayFunction();
		};
		Command redo = () ->
		{
			Map<Header, Object> line = convert(arr);
			this.innerList.add(index, line);
			displayFunction();
		};
		changed(true);
		addUndoRedo(undo, redo);
	}

	public void changeValue(String headerName, int indexRow, Object newValue)
	{
		if (this.headers == null)
		{
			return;
		}
		Map<Header, Object> row = this.innerList.get(indexRow);
		if (row == null)
		{
			return;
		}
		Header header = headerByName(headerName);
		Object oldValue = row.get(header);
		Command undo = () ->
		{
			row.remove(header);
			row.put(header, oldValue);
			displayFunction();
		};
		Command redo = () ->
		{
			row.remove(header);
			row.put(header, newValue);
			displayFunction();
		};
		addUndoRedo(undo, redo);
		changed(true);
	}

	public void setHeader(int index, String newName)
	{
		if (this.headers[index] != null && Str.areEqual(this.headers[index].name, newName))
		{
			return;
		}
		if (columnIsPresent(newName))
		{
			throw new ColumnIsPresentException(newName);
		}

		String oldName = this.headers[index].name;
		Command undo = () ->
		{
			this.headers[index].name = oldName;
			displayFunction();
		};
		Command redo = () ->
		{
			this.headers[index].name = newName;
			displayFunction();
		};
		changed(true);
		addUndoRedo(undo, redo);
	}

	public void extendsTable(int prefCols, int prefRows, BooleanSupplier supplier)
	{
		Table oldTable = new Table(this.evaluator);
		oldTable.fillFromTable(this);

		Command undo = () ->
		{
			this.headers = new ArrayList<Header>().toArray(new Header[0]);
			this.innerList.clear();
			this.fillFromTable(oldTable);
			displayFunction();
		};
		Command redo = () ->
		{
			if (prefCols < this.getHeaderSize() || prefRows < this.size())
			{
				if (supplier.getAsBoolean())
				{
					List<String> collect = IntStream.range(prefCols, this.getHeaderSize())
							.mapToObj(this::getHeader)
							.collect(Collectors.toList());
					this.removeColumns(collect.toArray(new String[collect.size()]));

					for (int i = this.size() - 1; i >= prefRows; i--)
					{
						this.remove(i);
					}
				}
			}
			else
			{
				IntStream.range(0, prefCols - this.getHeaderSize())
						.mapToObj(i -> Table.generateColumnName(this))
						.forEach(this::addColumns);

				IntStream.range(0, prefRows - this.size())
						.mapToObj(i -> IntStream.range(0, this.getHeaderSize())
								.mapToObj(j -> "")
								.collect(Collectors.toList()))
						.map(list -> list.toArray(new Object[list.size()]))
						.forEach(this::addValue);
			}
			displayFunction();
		};
		changed(true);
		addUndoRedo(undo, redo);
	}
	//endregion

	//region Listeners
	public void setChangeListener(ChangeListener changeListener)
	{
		this.changeListener = changeListener;
	}

	public void setUndoRedoFunction(BiConsumer<Command, Command> function)
	{
		this.undoRedoFunction = function;
	}

	public void setDisplayListener(Consumer<Table> consumer)
	{
		this.displayFunction = consumer;
	}
	//endregion

	//region private methods

	private void displayFunction()
	{
		Optional.ofNullable(this.displayFunction).ifPresent(df -> df.accept(this));
	}

	private void addUndoRedo(Command undo, Command redo)
	{
		Optional.ofNullable(this.undoRedoFunction).ifPresent(func -> func.accept(undo, redo));
	}

	private String[] convertHeaders(Parameters parameters, String[] headers, boolean withNumbers)
	{
		if (parameters == null)
		{
			return headers;
		}
		List<String> list = new ArrayList<>();
		if (withNumbers)
		{
			list.add("#");
		}
		list.addAll(parameters.values()
				.stream()
				.map(String::valueOf)
				.collect(Collectors.toList())
		);

		return list.toArray(new String[list.size()]);
	}

	private void addEmptyStringToAllLinesInNewColumn()
	{
		final String EMPTY_STRING = "";
		this.innerList.forEach(row ->
				Arrays.stream(this.headers)
					.filter(header -> !row.containsKey(header))
					.forEach(header -> row.put(header, EMPTY_STRING))
		);
	}


	private static ReportTable addMismatchedRow(ReportTable table, ReportBuilder report, Table differences, String name, String expectedValue, String actualValue)
	{
	    ReportTable result = table;
		if (result == null)
		{
		    result = report.addTable("Differences", null, true, 0, new int[]{10, 45, 45}, "#", "Expected", "Actual");
		}
		result.addValues(name, expectedValue, actualValue);
        differences.addValue(new String[] { expectedValue, actualValue } );
		
        return result; 
	}

	private Set<String> names(String[] exclude)
	{
		Set<String> set = new LinkedHashSet<String>();
		set.addAll(Arrays.asList(exclude));
		Set<String> names = new LinkedHashSet<String>();
		for (Header name : this.headers)
		{
			if (!set.contains(name.name))
			{
				names.add(name.name);
			}
		}

		return names;
	}

	private static Map<String, ArrayList<Object>> compareRows(Map<String, Object> thisRow, Map<String, Object> otherRow, Set<String> names)
	{
		Map<String, ArrayList<Object>> map = new LinkedHashMap<>();
		for (String name : names)
		{
			if (!thisRow.containsKey(name) || !otherRow.containsKey(name))
			{
				map.put(name, new ArrayList<>());
			}

			Object thisValue = thisRow.get(name);
			Object otherValue = otherRow.get(name);

			if (thisValue == null)
			{
				if (otherValue == null)
				{
					continue;
				}
				else if (!otherValue.equals(thisValue))
				{
					ArrayList<Object> objects = new ArrayList<>();
					objects.add(otherValue);
					objects.add(thisValue);
					map.put(name, objects);
				}
			}
			else if (!thisValue.equals(otherValue))
			{
				ArrayList<Object> objects = new ArrayList<>();
				objects.add(otherValue);
				objects.add(thisValue);
				map.put(name, objects);
			}
		}

		return map;
	}

	private void considerAs(Header.HeaderType type, String... columns) throws Exception
	{
		changed(true);

		int[] columnsIndexes = getIndexes(columns);
		for (int index : columnsIndexes)
		{
			this.headers[index].type = type;
		}
	}

	private int[] getIndexes(String... columns) throws Exception
	{
		int[] columnsIndexes = new int[columns.length];

		int count = 0;
		for (String column : columns)
		{
			boolean found = false;
			for (int i = 0; i < headers.length; i++)
			{
				if (this.headers[i].name.equals(column))
				{
					columnsIndexes[count++] = i;
					found = true;
					break;
				}
			}
			if (!found)
			{
				throw new Exception("Column '" + column + "' is not found");
			}
		}
		return columnsIndexes;
	}

	private Header headerByName(String name)
	{
		for (Header h : this.headers)
		{
			if (h.name.equals(name))
			{
				return h;
			}
		}
		return null;
	}

	private Map<Header, Object> convert(Object[] e)
	{
		Map<Header, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < Math.min(this.headers.length, e.length); i++)
		{
			map.put(this.headers[i], e[i]);
		}
		return map;
	}

	private Map<Header, Object> convert(Map<String, Object> e)
	{
		Map<Header, Object> map = new LinkedHashMap<>();
		Arrays.stream(this.headers).forEach(h -> map.put(h, null));
		
		for (Entry<String, Object> entry : e.entrySet())
		{
			Header key = headerByName(entry.getKey());
			if (key != null)
			{
				map.put(key, entry.getValue());
			}
		}
		return map;
	}

	private Collection<Map<Header, Object>> convert(Collection<? extends Map<String, Object>> e)
	{
		return e.stream().map(this::convert).collect(Collectors.toList());
	}

	private RowTable convertToStr(Map<Header, Object> map)
	{
		RowTable res = new RowTable(map);
		for (Entry<Header, Object> entry : map.entrySet())
		{
			Header header = entry.getKey();
			Object source = entry.getValue();
			res.put(header.name, convertCell(header, source, null));
		}
		return res;
	}
	
    private Object convertCell(Header header, Object source, ReportBuilder report)
    {
        if (header.type == null)
        {
            return source;
        }
        Object value = null;
        try
        {
            if (header.type == Header.HeaderType.EXPRESSION && this.evaluator != null)
            {
                value = this.evaluator.evaluate("" + source);
            }
            else if (header.type == Header.HeaderType.GROUP)
            {
                if (report != null)
                {
                    AtomicInteger level = new AtomicInteger(0);
                    StringBuilder group = new StringBuilder();
                    boolean isNode = extract(Str.asString(source), group, level);
                    value = report.decorateGroupCell(group, level.get(), isNode);
                }
                else
                {
                    value = "" + source;
                }
            }
            else
            {
                value = Converter.convertToType(source, header.type.clazz);
            }
        }
        catch (Exception e)
        {
            value = e.getMessage();
        }

        return value;
    }
	
	private boolean extract(String path, StringBuilder group,  AtomicInteger outLevel)
	{
	    String[] parts = path.split("/");
	    int last = parts.length - 1;
	    if (parts[last].equals("*"))
	    {
	        group.append(parts[last - 1]);
	        outLevel.set(Math.max(0, parts.length - 2));
	        return true;
	    }
	    else
	    {
	        group.append(parts[last]);
            outLevel.set(Math.max(0, parts.length - 1));
	        return false;
	    }
	}
	
	private class TableListIterator implements ListIterator<RowTable>
	{
		private ListIterator<Map<Header, Object>> iterator;

		public TableListIterator(ListIterator<Map<Header, Object>> iterator)
		{
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext()
		{
			return this.iterator.hasNext();
		}

		@Override
		public RowTable next()
		{
			return convertToStr(this.iterator.next());
		}

		@Override
		public boolean hasPrevious()
		{
			return this.iterator.hasPrevious();
		}

		@Override
		public RowTable previous()
		{
			return convertToStr(this.iterator.previous());
		}

		@Override
		public int nextIndex()
		{
			return this.iterator.nextIndex();
		}

		@Override
		public int previousIndex()
		{
			return this.iterator.previousIndex();
		}

		@Override
		public void remove()
		{
			this.iterator.remove();
		}

		@Override
		public void set(RowTable stringObjectMap)
		{
			this.iterator.set(convert(stringObjectMap));
		}

		@Override
		public void add(RowTable stringObjectMap)
		{
			this.iterator.add(convert(stringObjectMap));
		}
	}

	private void read(Reader reader, char delimiter) throws Exception
	{
		CsvReader csvReader = null;

		try
		{
			csvReader = new CsvReader(reader);
			csvReader.setSkipEmptyRecords(true);
			csvReader.setDelimiter(delimiter);

			this.headers = null;
			String[] str;

			if (csvReader.readRecord())
			{
				addColumns(csvReader.getValues());

				while (csvReader.readRecord())
				{
					str = csvReader.getValues();

					Map<Header, Object> line = new LinkedHashMap<>();

					for (int i = 0; i < headers.length; i++)
					{
						if (str.length <= i)
						{
							break;
						}
						line.put(headers[i], str[i]);
					}
					this.innerList.add(line);
				}
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (csvReader != null)
			{
				csvReader.close();
			}
		}
	}

	private void readFilesInfo(String dirName) throws Exception
	{
		try
		{
			File directory = new File(dirName);

				this.headers = null;
				addColumns("Name", "Size", "Date", "Is directory", "Hidden");
				this.considerAsString("Name");
				this.considerAsBoolean("Is directory", "Hidden");
				this.considerAsDouble("Size");
				this.considerAsDate("Date");
				File[] files = directory.listFiles();
				for (File file : files)
				{
					Map<Header, Object> line = new LinkedHashMap<>();
					line.put(headers[0], file.getName());
					line.put(headers[1], file.length());
					line.put(headers[2], new Date(file.lastModified()));
					line.put(headers[3], file.isDirectory());
					line.put(headers[4], file.isHidden());
					this.innerList.add(line);
				}

		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	private List<Header> filter(String ... columns)
	{
		Set<String> set = new HashSet<String>(Arrays.asList(columns));
		
		return Arrays.stream(this.headers)
				.filter(h -> set.contains(h.name))
				.collect(Collectors.toList());
	}
	
    private static boolean areEqual(Object s1, Object s2)
    {
    	if (s1 == null)
    	{
    		return s1 == s2;
    	}
    	
    	return s1.equals(s2);
    }
	//endregion
}
