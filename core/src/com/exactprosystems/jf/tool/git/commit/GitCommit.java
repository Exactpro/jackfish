////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2016, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////
package com.exactprosystems.jf.tool.git.commit;

import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.git.CredentialBean;
import com.exactprosystems.jf.tool.git.GitBean;
import com.exactprosystems.jf.tool.git.GitUtil;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import com.exactprosystems.jf.tool.main.Main;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class GitCommit
{
	private static final Logger logger = Logger.getLogger(GitCommit.class);

	private Main model;
	private GitCommitController controller;

	private Service<Void> service;

	public GitCommit(Main model, List<GitBean> list) throws Exception
	{
		this.model = model;
		this.controller = Common.loadController(this.getClass().getResource("GitCommit.fxml"));
		this.controller.init(this, list);
	}

	public void close() throws Exception
	{
		if (this.service == null || !this.service.isRunning())
		{
			this.controller.hide();
		}
		if (this.service != null && this.service.isRunning())
		{
			this.service.cancel();
			this.service = null;
		}
	}

	public void commit(String msg, List<GitBean> list, boolean isAmend) throws Exception
	{
		this.commitOrPush(msg, list, true, isAmend);
	}

	public void push(String msg, List<GitBean> list, boolean isAmend) throws Exception
	{
		this.commitOrPush(msg, list, false, isAmend);
	}

	private void commitOrPush(String msg, List<GitBean> list, boolean isCommit, boolean isAmend) throws Exception
	{
		this.controller.setDisable(true);
		CredentialBean credential = model.getCredential();
		this.service = new Service<Void>()
		{
			@Override
			protected Task<Void> createTask()
			{
				return new Task<Void>()
				{
					@Override
					protected Void call() throws Exception
					{
						DialogsHelper.showInfo("Start commitinging");
						GitUtil.gitCommit(credential, list.stream().map(GitBean::getFile).collect(Collectors.toList()), msg, isAmend);
						return null;
					}
				};
			}
		};
		service.start();
		service.setOnSucceeded(e -> {
			this.controller.setDisable(false);
			DialogsHelper.showSuccess("Successful commiting");
			this.controller.hide();
			if (!isCommit)
			{
				Common.tryCatch(this.model::gitPush, "Error on push");
			}
		});
		service.setOnCancelled(e -> {
			DialogsHelper.showInfo("Task canceled by user");
			this.controller.setDisable(false);
		});
		service.setOnFailed(e ->
		{
			Throwable exception = e.getSource().getException();
			logger.error(exception.getMessage(), exception);
			DialogsHelper.showError("Error on commiting\n" + exception.getMessage());
			this.controller.setDisable(false);
		});
	}


	public void display()
	{
		this.controller.show();
	}
}
