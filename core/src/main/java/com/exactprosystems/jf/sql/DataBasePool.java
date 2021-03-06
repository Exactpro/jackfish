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

package com.exactprosystems.jf.sql;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.error.JFSQLException;
import com.exactprosystems.jf.common.MainRunner;
import com.exactprosystems.jf.documents.DocumentFactory;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.documents.config.SqlEntry;
import com.exactprosystems.jf.functions.Table;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;

public class DataBasePool
{
	private static final String USER     = "user";
	private static final String PASSWORD = "password";

	private final DocumentFactory     factory;
	private final Map<String, Driver> drivers;

	public DataBasePool(DocumentFactory factory)
	{
		this.factory = factory;
		this.drivers = new HashMap<>();
	}

	public SqlConnection connect(String sql, String server, String base, String user, String password) throws JFSQLException
	{
		Driver driver = this.getDriver(sql);
		Connection connection = this.connection(sql, driver, server, base, user, password);
		return new SqlConnection(connection, sql);
	}

	public Table select(SqlConnection connection, String text, Object[] objs) throws JFSQLException
	{
		try (PreparedStatement query = connection.getConnection().prepareStatement(text, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
		{
			this.fillParameters(objs, query);

			try (ResultSet result = query.executeQuery())
			{
				return new Table(result, null);
			}
		}
		catch (SQLException e)
		{
			throw new JFSQLException(e.getMessage(), e);
		}
	}

	public boolean execute(SqlConnection connection, String text, Object[] objs) throws JFSQLException
	{
		try (PreparedStatement query = connection.getConnection().prepareStatement(text, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
		{
			this.fillParameters(objs, query);
			return query.execute();
		}
		catch (SQLException e)
		{
			throw new JFSQLException(e);
		}
	}

	public List<Integer> insert(SqlConnection connection, String text, Object[] objs) throws JFSQLException
	{
		try (PreparedStatement query = connection.getConnection().prepareStatement(text, Statement.RETURN_GENERATED_KEYS))
		{
			this.fillParameters(objs, query);

			query.execute();

			List<Integer> indexes = new ArrayList<>();
			try (ResultSet set = query.getGeneratedKeys())
			{
				int i = 1;
				while (set.next())
				{
					indexes.add(set.getInt(i++));
				}
			}

			return indexes;
		}
		catch (SQLException e)
		{
			throw new JFSQLException(e);
		}
	}

	public void disconnect(SqlConnection connection) throws JFSQLException
	{
		try
		{
			connection.close();
		}
		catch (SQLException e)
		{
			throw new JFSQLException(e);
		}
	}

	private Connection connection(String sql, Driver driver, String server, String base, String user, String password) throws JFSQLException
	{
		try
		{
			SqlEntry entry = this.factory.getConfiguration().getSqlEntry(sql);
			String url = entry.get(Configuration.sqlConnection);
			String connectionString = url.replace("${SERVER}", server).replace("${BASE}", base);
			Properties prop = new Properties();
			prop.put(USER, user);
			prop.put(PASSWORD, password);
			return driver.connect(connectionString, prop);
		}
		catch (Exception e)
		{
			throw new JFSQLException(e.getMessage(), e);
		}
	}

	//region private methods
	private Driver getDriver(String sql) throws JFSQLException
	{
		if (sql == null)
		{
			throw new JFSQLException(R.DATA_BASE_POOL_SQL_ID_NULL.get());
		}

		Driver driver = this.drivers.get(sql);
		if (driver == null)
		{
			try
			{
				SqlEntry entry = this.factory.getConfiguration().getSqlEntry(sql);
				if (entry == null)
				{
					throw new JFSQLException(sql + R.COMMON_IS_NOT_FOUND.get());
				}

				String jarName = MainRunner.makeDirWithSubstitutions(entry.get(Configuration.sqlJar));
				URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:" + jarName)});

				ServiceLoader<Driver> loader = ServiceLoader.load(Driver.class, classLoader);
				Iterator<Driver> iterator = loader.iterator();

				if (iterator.hasNext())
				{
					driver = iterator.next();
				}

				if (driver == null)
				{
					throw new JFSQLException(String.format(R.DATA_BASE_POOL_JAR_DOESNT_CONTAINS.get(), jarName));
				}

				this.drivers.put(sql, driver);
				DriverManager.registerDriver(driver);
			}
			catch (Exception e)
			{
				throw new JFSQLException(e.getMessage(), e);
			}
		}
		return driver;
	}

	private void fillParameters(Object[] objs, PreparedStatement query) throws JFSQLException
	{
		try
		{
			int index = 1;
			int limit = query.getParameterMetaData().getParameterCount();
			if (limit != objs.length)
			{
				throw new JFSQLException(R.DATA_BASE_POOL_NUMBER_OF_PARAMS.get());
			}
			for (Object obj : objs)
			{
				if (index <= limit)
				{
					query.setObject(index, obj);
				}
				index++;
			}
		}
		catch (SQLException e)
		{
			throw new JFSQLException(e);
		}
	}
	//endregion
}
