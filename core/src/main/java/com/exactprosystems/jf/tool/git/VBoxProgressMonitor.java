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
package com.exactprosystems.jf.tool.git;

import com.exactprosystems.jf.tool.Common;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.eclipse.jgit.lib.BatchingProgressMonitor;

public class VBoxProgressMonitor extends BatchingProgressMonitor
{
	private VBox box;
	private int currentIndex = 0;

	public VBoxProgressMonitor(VBox box)
	{
		this.box = box;
	}

	public void clear()
	{
		this.box.getChildren().forEach(n -> n = null);
		this.box.getChildren().clear();
		this.currentIndex = 0;
	}

	@Override
	protected void onUpdate(String taskName, int workCurr)
	{
		StringBuilder s = new StringBuilder();
		format(s, taskName, workCurr);
		send(s);
	}

	@Override
	protected void onEndTask(String taskName, int workCurr)
	{
		StringBuilder s = new StringBuilder();
		format(s, taskName, workCurr);
		send(s);
	}

	private void format(StringBuilder s, String taskName, int workCurr)
	{
		s.append(taskName);
		s.append(": ");
		while (s.length() < 25)
			s.append(' ');
		s.append(workCurr);
	}

	@Override
	protected void onUpdate(String taskName, int cmp, int totalWork, int pcnt)
	{
		StringBuilder s = new StringBuilder();
		format(s, taskName, cmp, totalWork, pcnt);
		send(s);
	}

	@Override
	protected void onEndTask(String taskName, int cmp, int totalWork, int pcnt)
	{
		StringBuilder s = new StringBuilder();
		format(s, taskName, cmp, totalWork, pcnt);
		currentIndex++;
		send(s);
	}

	private void format(StringBuilder s, String taskName, int cmp, int totalWork, int pcnt)
	{
		s.append(taskName);
		s.append(": ");
		while (s.length() < 25)
			s.append(' ');

		String endStr = String.valueOf(totalWork);
		String curStr = String.valueOf(cmp);
		while (curStr.length() < endStr.length())
			curStr = " " + curStr;
		if (pcnt < 100)
			s.append(' ');
		if (pcnt < 10)
			s.append(' ');
		s.append(pcnt);
		s.append("% (");
		s.append(curStr);
		s.append("/");
		s.append(endStr);
		s.append(")");
	}

	private void send(StringBuilder s)
	{
		Common.runLater(() -> {
			ObservableList<Node> children = this.box.getChildren();
			if (currentIndex > children.size() - 1)
			{
				children.add(new Text());
			}
			((Text) children.get(children.size() - 1)).setText(s.toString());
		});
	}
}
