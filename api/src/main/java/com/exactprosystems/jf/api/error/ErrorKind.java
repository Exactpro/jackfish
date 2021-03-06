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

package com.exactprosystems.jf.api.error;

import com.exactprosystems.jf.api.common.i18n.R;

public enum ErrorKind
{
	EXCEPTION				(R.ERROR_KIND_EXCEPTION),
	EXPRESSION_ERROR		(R.ERROR_KIND_EXPRESSION_ERROR),
	ASSERT					(R.ERROR_KIND_ASSERT_IS_FALSE),
	EMPTY_PARAMETER			(R.ERROR_KIND_EMPTY_PARAMETER),
	WRONG_PARAMETERS		(R.ERROR_KIND_WRONG_PARAMETERS),
	INPUT_CANCELLED			(R.ERROR_KIND_INPUT_CANCELED),
    ROW_EXPIRED             (R.ERROR_KIND_ROW_EXPIRED),
	LOCATOR_NOT_FOUND		(R.ERROR_KIND_LOCATOR_NOT_FOUND),
	DIALOG_NOT_FOUND		(R.ERROR_KIND_DIALOG_NOT_FOUND),
	DIALOG_CHECK_LAYOUT		(R.ERROR_KIND_DIALOG_CHECK_LAYOUT),
	ELEMENT_NOT_FOUND		(R.ERROR_KIND_ELEMENT_NOT_FOUND),
	TOO_MANY_ELEMENTS		(R.ERROR_KIND_TOO_MANY_ELEMENTS),
	FEATURE_NOT_SUPPORTED	(R.ERROR_KIND_FEATURE_NOT_SUPPORTED),
	OPERATION_NOT_ALLOWED	(R.ERROR_KIND_OPERATION_NOT_ALLOWED),
	ELEMENT_NOT_ENABLED		(R.ERROR_KIND_ELEMENT_NOT_ENABLED),
	OPERATION_FAILED		(R.ERROR_KIND_OPERATION_FAILED),
	NOT_EQUAL				(R.ERROR_KIND_NOT_EQUAL),
	SQL_ERROR				(R.ERROR_KIND_SQL_ERROR),
	SERVICE_ERROR			(R.ERROR_KIND_SERVICE_ERROR),
	APPLICATION_ERROR		(R.ERROR_KIND_APPLICATION_ERROR),
	CLIENT_ERROR			(R.ERROR_KIND_CLIENT_ERROR),
	TIMEOUT					(R.ERROR_KIND_TIMEOUT),
	MANY_ERRORS				(R.ERROR_KIND_MANY_ERRORS),
	FAIL					(R.ERROR_KIND_FAIL),
	OTHER					(R.ERROR_KIND_OTHER),
	CHART_EXCEPTION			(R.ERROR_KIND_CHART_EXCEPTION),
	MATRIX_ERROR			(R.ERROR_KIND_MATRIX_ERROR),
    CONTROL_NOT_SUPPORTED	(R.ERROR_KIND_CONTROL_NOT_SUPPORTED),
	APPLICATION_CLOSED		(R.ERROR_KIND_APPLICATION_CLOSED);

	
	ErrorKind(R name)
	{
		this.name = name;
	}
	
	@Override
	public String toString()
	{
		return this.name.get();
	}
	
	private R name;
}
