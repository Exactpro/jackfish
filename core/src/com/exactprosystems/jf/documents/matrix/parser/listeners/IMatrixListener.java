package com.exactprosystems.jf.documents.matrix.parser.listeners;

import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.matrix.parser.Result;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;

public interface IMatrixListener extends Cloneable
{
	void		reset				(Matrix matrix);
	void		matrixStarted		(Matrix matrix);
	void		matrixFinished		(Matrix matrix, int passed, int failed);
	void		started				(Matrix matrix, MatrixItem item);
	void		paused				(Matrix matrix, MatrixItem item);
	void		finished			(Matrix matrix, MatrixItem item, Result result);
	void		error				(Matrix matrix, int lineNumber, MatrixItem item, String message);
	String		getExceptionMessage	();
	boolean		isOk				();

}
