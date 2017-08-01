package com.exactprosystems.jf.tool.search;

import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.newconfig.ConfigurationFx;

import java.io.File;

class SearchResult
{
	private File file;
	private int lineNumber;

	public SearchResult(File file, int lineNumber)
	{
		this.file = file;
		this.lineNumber = lineNumber;
	}

	public File getFile()
	{
		return file;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	@Override
	public String toString()
	{
		return "File : " + Common.getRelativePath(file.getPath()) + " : " + lineNumber;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		SearchResult that = (SearchResult) o;

		if (lineNumber != that.lineNumber)
		{
			return false;
		}
		return file != null ? ConfigurationFx.path(file).equals(ConfigurationFx.path(that.file)) : that.file == null;
	}

	@Override
	public int hashCode()
	{
		int result = file != null ? file.hashCode() : 0;
		result = 31 * result + lineNumber;
		return result;
	}

	static class FailedSearchResult extends SearchResult
	{
		private String failMessage;

		public FailedSearchResult(String failMessage)
		{
			super(null, 0);
			this.failMessage = failMessage;
		}

		@Override
		public String toString()
		{
			return this.failMessage;
		}
	}
}
