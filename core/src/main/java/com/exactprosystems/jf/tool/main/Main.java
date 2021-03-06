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

package com.exactprosystems.jf.tool.main;

import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.api.wizard.WizardManager;
import com.exactprosystems.jf.common.CommonHelper;
import com.exactprosystems.jf.common.MainRunner;
import com.exactprosystems.jf.common.MutableString;
import com.exactprosystems.jf.common.Settings;
import com.exactprosystems.jf.common.Settings.SettingsValue;
import com.exactprosystems.jf.common.version.VersionInfo;
import com.exactprosystems.jf.documents.*;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.csv.Csv;
import com.exactprosystems.jf.documents.guidic.GuiDictionary;
import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.text.PlainText;
import com.exactprosystems.jf.documents.vars.SystemVars;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.custom.store.StoreVariable;
import com.exactprosystems.jf.tool.custom.tab.CustomTab;
import com.exactprosystems.jf.tool.documents.guidic.DictionaryFx;
import com.exactprosystems.jf.tool.documents.FxDocumentFactory;
import com.exactprosystems.jf.tool.git.CredentialBean;
import com.exactprosystems.jf.tool.git.CredentialDialog;
import com.exactprosystems.jf.tool.git.GitBean;
import com.exactprosystems.jf.tool.git.GitUtil;
import com.exactprosystems.jf.tool.git.branch.GitBranch;
import com.exactprosystems.jf.tool.git.clone.GitClone;
import com.exactprosystems.jf.tool.git.commit.GitCommit;
import com.exactprosystems.jf.tool.git.merge.GitMerge;
import com.exactprosystems.jf.tool.git.merge.GitMergeBean;
import com.exactprosystems.jf.tool.git.pull.GitPull;
import com.exactprosystems.jf.tool.git.push.GitPush;
import com.exactprosystems.jf.tool.git.reset.GitReset;
import com.exactprosystems.jf.tool.git.status.GitStatus;
import com.exactprosystems.jf.tool.git.tag.GitTag;
import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import com.exactprosystems.jf.tool.helpers.DialogsHelper.OpenSaveMode;
import com.exactprosystems.jf.tool.matrix.MatrixFx;
import com.exactprosystems.jf.tool.newconfig.ConfigurationFx;
import com.exactprosystems.jf.tool.newconfig.wizard.WizardConfiguration;
import com.exactprosystems.jf.tool.search.Search;
import com.exactprosystems.jf.api.common.i18n.Locales;
import com.exactprosystems.jf.tool.settings.Theme;
import com.exactprosystems.jf.tool.wizard.WizardManagerImpl;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Main extends Application
{
	public static boolean needForget = false;
	public static final String DIMENSION_AND_POSITION = "dimensionAndPosition";
	private static final String SEPARATOR = ";";
	private static final String SMALL_SEPARATOR = "x";
	private static final Logger logger = Logger.getLogger(Main.class);

	public static boolean IS_PROJECT_UNDER_GIT;

	private static String configName = null;

	private MainController controller;

	private Configuration config;
	private Settings settings;
	private DocumentFactory	factory;
	private WizardManager wizardManager;

	private String username;
	private String password;

	private List<String> toolbarMatrices = new ArrayList<>();

	private boolean isFromInit = true;
	private List<Document> needDisplayDoc = new ArrayList<>();
	private boolean showWaits = true;

	public static String getConfigName()
	{
		String temp = configName;
		configName = null;
		return temp;
	}

	//region public methods
	public void setConfiguration(Configuration config)
	{
		this.config = config;
		this.factory.setConfiguration(this.config);
		if (this.controller != null)
		{
			this.controller.disableMenu(this.config == null);
		}
	}
	//endregion

	//region Application
	@Override
	public void init() throws Exception
	{
		notifyPreloader(new Preloader.ProgressNotification(0));
		try
		{
			this.wizardManager = new WizardManagerImpl();
			this.factory = new FxDocumentFactory(this, this.wizardManager);
			this.settings = this.factory.getSettings();
			DialogsHelper.setTimeNotification(Integer.parseInt(this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.TIME_NOTIFICATION).getValue()));
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			DialogsHelper.showError(R.MAIN_INVALID_SETTINGS.get());
			this.settings = Settings.defaultSettings();
		}

		Locales.setDefault(this.settings.getValue(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.LANGUAGE).getValue());

		Settings.SettingsValue theme = this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.THEME);
		Theme.setCurrentTheme(theme.getValue());

		notifyPreloader(new Preloader.ProgressNotification(5));

		controller = Common.loadController(Main.class.getResource("tool.fxml"));

		notifyPreloader(new Preloader.ProgressNotification(15));

		controller.disableMenu(true);
		IS_PROJECT_UNDER_GIT = GitUtil.isGitRepository();
		controller.isGit(IS_PROJECT_UNDER_GIT);

		notifyPreloader(new Preloader.ProgressNotification(20));

		final List<String> args = getParameters().getRaw();

		if (args.size() > 0)
		{
			Main.this.username = args.size() > 1 ? args.get(1) : null;
			Main.this.password = args.size() > 2 ? args.get(2) : null;

			openProject(args.get(0), controller.projectPane); // TODO
			notifyPreloader(new Preloader.ProgressNotification(30));
			if (this.config instanceof ConfigurationFx)
			{
				this.needDisplayDoc.add(this.config);
			}
			controller.clearLastMatrixMenu();

			Collection<SettingsValue> list = settings.getValues(Settings.MAIN_NS, DocumentKind.MATRIX.toString());
			controller.updateFileLastMatrix(list);
			notifyPreloader(new Preloader.ProgressNotification(35));
		}
		notifyPreloader(new Preloader.ProgressNotification(40));

		notifyPreloader(new Preloader.ProgressNotification(50));
		try
		{
			for (SettingsValue item : settings.getValues(Settings.MAIN_NS, Settings.MATRIX_TOOLBAR))
			{
				this.addToToolbar(item.getKey(), item.getValue());
			}
			notifyPreloader(new Preloader.ProgressNotification(55));

			List<SettingsValue> values = settings.getValues(Settings.MAIN_NS, Settings.OPENED);
			double progressStep = 45 /(double) Math.max(1, values.size());
			double currentProgress = 55;
			for (SettingsValue item : values)
			{
				DocumentKind kind = DocumentKind.valueOf(item.getValue());
				String filePath = item.getKey();
				File file = new File(filePath);
				try
				{
					needDisplayDoc.add(loadDocument(file, kind));
				}
				catch (FileNotFoundException e)
				{
					settings.remove(Settings.MAIN_NS, Settings.OPENED, file.getAbsolutePath());
					settings.saveIfNeeded();
				}
				currentProgress += progressStep;
				notifyPreloader(new Preloader.ProgressNotification(currentProgress));
			}
		}
		catch (Exception e)
		{
			logger.error(R.MAIN_ERROR_ON_RESTORE_DOCS.get());
			logger.error(e.getMessage(), e);
		}
		notifyPreloader(new Preloader.ProgressNotification(100));
		Thread.sleep(200);
		notifyPreloader(new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_START));
	}

	@Override
	public void start(final Stage stage) throws Exception
	{
		loadDimensionAndPosition((w, h) ->
		{
			stage.setWidth(w);
			stage.setHeight(h);
		}, (x, y) ->
		{
			stage.setX(x);
			stage.setY(y);
		});
		controller.init(factory, this, this.wizardManager, settings, stage);
		Common.node = stage;
		Common.reportListener(url -> HostServicesFactory.getInstance(this).showDocument(url));
		controller.display();
		controller.initShortcuts();
		this.isFromInit = false;
		for (Document document : this.needDisplayDoc)
		{
			if (document != null)
			{
				if (DocumentKind.byDocument(document).isUseNewMVP())
				{
					this.factory.showDocument(document);
				}
				else
				{
					document.display();
				}
			}
		}
		this.needDisplayDoc.clear();
	}
	//endregion

	//region Configuration
	public void openProject(String filePath, BorderPane pane) throws Exception
	{
		Optional<File> optional = chooseFile(Configuration.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			File file = optional.get();
			if (this.config != null)
			{
				if (this.config.canClose())
				{
					this.config.close();
					setConfiguration(null);
				}
				else
				{
					return;
				}
			}

			Path path = MainRunner.needToChangeDirectory(file.getPath());
			if (path != null)
			{
				configName = file.getAbsolutePath();
				this.controller.close();
			}

			Configuration config = (Configuration)loadDocument(file, DocumentKind.CONFIGURATION);
            if (config instanceof ConfigurationFx)
            {
                setConfiguration(config);
				((ConfigurationFx) config).setPane(pane);
				config.refresh();
			}
		}
	}

	public void createNewProject(BorderPane pane) throws Exception
	{
		WizardConfiguration wizard = new WizardConfiguration(this);
		String fullPath = wizard.display();
		if (fullPath != null)
		{
			if (this.config != null)
			{
				if (this.config.canClose())
				{
					this.config.close();
					setConfiguration(null);
				}
				else
				{
					return;
				}
			}
			File newFolder = new File(fullPath);
			String configName = newFolder.getName();
			String configurePath = fullPath + File.separator + configName + ".xml";

			Configuration newConfig = Configuration.createNewConfiguration(configName, this.factory);
			newConfig.save(configurePath);
			openProject(configurePath, pane);
		}
	}

	public void projectFromGit(BorderPane projectPane) throws Exception
	{
		GitClone cloneWindow = new GitClone(this.getCredential());
		String fullPath = cloneWindow.display();
		if (fullPath != null)
		{
			openProject(fullPath, projectPane);
		}
	}

	public void refreshConfig() throws Exception
	{
		if (this.config != null)
		{
			this.config.refresh();
		}
	}

	public void saveConfig() throws Exception
	{
		if (this.config != null)
		{
			this.config.save(this.config.getNameProperty().get());
		}
	}
	//endregion

	//region Git
	public CredentialBean getCredential()
	{
		checkCredential();
		SettingsValue idRsa = this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.GIT, Settings.GIT_SSH_IDENTITY);
		SettingsValue knownHosts = this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.GIT, Settings.GIT_KNOWN_HOST);
		return new CredentialBean(this.username, this.password, idRsa.getValue(), knownHosts.getValue());
	}

	public void saveCredential(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	public void changeCredential()
	{
		new CredentialDialog(this::storeCredential).display(this.username, this.password);
	}

	public void gitStatus() throws Exception
	{
		CredentialBean credential = getCredential();
		new GitStatus(this).display(GitUtil.gitStatus(credential), GitUtil.gitState(credential));
	}

	public List<String> gitMerge() throws Exception
	{
		List<GitMergeBean> collect = GitUtil.gitStatus(getCredential())
				.stream()
				.filter(gb -> gb.getStatus() == GitBean.Status.CONFLICTING)
				.map(GitBean::getFile)
				.map(File::getAbsolutePath)
				.map(Common::getRelativePath)
				.map(GitMergeBean::new)
				.collect(Collectors.toList());
		GitMerge gitMerge = new GitMerge(this, collect);
		gitMerge.display();
		return gitMerge.getMergedFiles();
	}

	public List<String> getMergeFiles() throws Exception
	{
		return GitUtil.gitStatus(getCredential())
				.stream()
				.filter(gb -> gb.getStatus() == GitBean.Status.CONFLICTING)
				.map(GitBean::getFile)
				.map(File::getAbsolutePath)
				.collect(Collectors.toList());

	}

	public void gitBranches() throws Exception
	{
		new GitBranch(this.getCredential()).display();
	}

	public void gitPush() throws Exception
	{
		new GitPush(this).display();
	}

	public void gitTags() throws Exception
	{
		new GitTag(this).display();
	}

	public void gitPull() throws Exception
	{
		new GitPull(this).display();
	}

	public void gitCommit() throws Exception
	{
		CredentialBean credential = getCredential();
		new GitCommit(this, GitUtil.gitStatus(credential)).display();
	}

	public void gitReset() throws Exception
	{
		new GitReset(this, GitUtil.gitGetCommits(getCredential())).display();
	}

	//endregion

	//region Load documents
	public void loadDictionary(String filePath, String entryName) throws Exception
	{
		checkConfig();
		Optional<File> optional = chooseFile(GuiDictionary.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			Document dictionary = loadDocument(optional.get(), DocumentKind.GUI_DICTIONARY);
			if (dictionary instanceof DictionaryFx)
			{
				((DictionaryFx) dictionary).setCurrentApp(entryName);
			}
		}
	}

	public void loadMatrix(String filePath) throws Exception
	{
		checkConfig();
		Optional<File> optional = chooseFile(Matrix.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			loadDocument(optional.get(), DocumentKind.MATRIX);
		}
	}

	public void loadSystemVars(String filePath) throws Exception
	{
		checkConfig();
		Optional<File> optional = chooseFile(SystemVars.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			loadDocument(optional.get(), DocumentKind.SYSTEM_VARS);
		}
	}

	public void loadPlainText(String filePath) throws Exception
	{
		checkConfig();
		Optional<File> optional = chooseFile(PlainText.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			loadDocument(optional.get(), DocumentKind.PLAIN_TEXT);
		}
	}

	public void loadCsv(String filePath) throws Exception
	{
		checkConfig();
		Optional<File> optional = chooseFile(Csv.class, filePath, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			loadDocument(optional.get(), DocumentKind.CSV);
		}
	}
	//endregion

	//region Create documents
	public void newDictionary() throws Exception
	{
		newDocument(DocumentKind.GUI_DICTIONARY, newName(GuiDictionary.class), doc -> {});
	}

	public void newMatrix() throws Exception
	{
		newDocument(DocumentKind.MATRIX, newName(Matrix.class), doc ->
		{
			Settings.SettingsValue copyright = settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.COPYRIGHT);
			String text = copyright.getValue().replaceAll("\\\\n", System.lineSeparator());
			((Matrix) doc).addCopyright(text);
			((Matrix) doc).enumerate();
		});
	}

	public void newLibrary(String fullPath) throws Exception
	{
		checkConfig();
		Matrix doc = (Matrix) this.factory.createDocument(DocumentKind.LIBRARY, fullPath);
		doc.create();
		Settings.SettingsValue copyright = settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.COPYRIGHT);
		String text = copyright.getValue().replaceAll("\\\\n", System.lineSeparator());
		doc.addCopyright(text);
		if (new File(fullPath).exists())
		{
			new File(fullPath).createNewFile();
		}
		if (!fullPath.equals(newName(Matrix.class)))
		{
			doc.save(fullPath);
		}
		doc.enumerate();
		this.factory.showDocument(doc);
	}

	public void newLibrary() throws Exception
	{
		newLibrary(newName(Matrix.class));
	}

	public void newSystemVars() throws Exception
	{
		newDocument(DocumentKind.SYSTEM_VARS, newName(SystemVars.class), doc -> {});
	}

	public void newPlainText() throws Exception
	{
		newDocument(DocumentKind.PLAIN_TEXT, newName(PlainText.class), doc -> {});
	}

	public void newCsv() throws Exception
	{
		newDocument(DocumentKind.CSV, newName(Csv.class), doc -> {});
	}

	private void newDocument(DocumentKind kind, String name, Consumer<Document> consumer) throws Exception
	{
		checkConfig();
		Document document = this.factory.createDocument(kind, name);
		document.create();
		consumer.accept(document);
		this.factory.showDocument(document);
	}
	//endregion

	//region Documents
	public void documentSaveAs(Document document) throws Exception
	{
		if (document == null)
		{
			DialogsHelper.showInfo(R.MAIN_NOTHING_TO_SAVE.get());
			return;
		}
		File file = DialogsHelper.showSaveAsDialog(document);
		if (file != null)
		{
			String lastName = document.getNameProperty().get();
			String newName = file.getPath();

			document.save(file.getPath());
			document.saved();
			DialogsHelper.showInfo(String.format(R.MAIN_SAVE_SUCCESS.get(), document.getNameProperty().get()));
			if (document instanceof Matrix)
			{
				this.settings.remove(Settings.MAIN_NS, DocumentKind.MATRIX.name(), new File(lastName).getName());
				this.settings.setValue(Settings.MAIN_NS, DocumentKind.MATRIX.name(), new File(document.getNameProperty().get()).getName(), newName);
				this.settings.saveIfNeeded();
				this.controller.updateFileLastMatrix(this.settings.getValues(Settings.MAIN_NS, DocumentKind.MATRIX.name()));
			}
		}
	}

	public void documentSave(Document document) throws Exception
	{
		if (document == null)
		{
			return;
		}

		if (document.getNameProperty().isNullOrEmpty() || !(new File(document.getNameProperty().get()).exists()))
		{
			documentSaveAs(document);
		}
		else
		{
			document.save(document.getNameProperty().get());
			document.saved();
			DialogsHelper.showInfo(String.format(R.MAIN_SAVE_SUCCESS.get(), document.getNameProperty().get()));
		}
	}

	public void documentsSaveAll() throws Exception
	{
		for (Document document : this.config.getSubordinates())
		{
			documentSave(document);
		}
		DialogsHelper.showSuccess(R.MAIN_ALL_FILES_SUCCESS.get());
	}

	public void undo(Document document) throws Exception
	{
		if (document != null)
		{
			document.undo();
		}
	}

	public void redo(Document document) throws Exception
	{
		if (document != null)
		{
			document.redo();
		}
	}

	public void changeDocument(Document document)
	{
		StringBuilder sb = new StringBuilder(Configuration.projectName);
		sb.append(" ").append(VersionInfo.getVersion());
		if (this.config != null)
        {
            sb.append(" [ ").append(this.config.getNameProperty().get().replace(".xml", "")).append(" ]");
        }

		if (document != null)
		{
			File file = new File(document.getNameProperty().get());
			String absolutePath = file.getAbsolutePath();
			sb.append(" [ ").append(absolutePath).append(" ]");
		}
		this.controller.displayTitle(sb.toString());
	}
	//endregion

	//region Reports
	public void openReport() throws Exception
	{
		File file = DialogsHelper.showOpenSaveDialog(R.MAIN_OPEN_REPORT_TITLE.get(), R.MAIN_OPEN_REPORT_FILTER.get(), "*.html", OpenSaveMode.OpenFile);
		openReport(file);
	}

	public void openReport(File file)
	{
		Optional.ofNullable(file).ifPresent(f -> DialogsHelper.displayReport(f, null, this.factory));
	}
	//endregion

	//region Matrix
	public void runMatrixFromFile() throws Exception
	{
		Optional<File> optional = chooseFile(Matrix.class, null, DialogsHelper.OpenSaveMode.OpenFile);
		if (optional.isPresent())
		{
			runMatrixFromFile(optional.get());
		}
	}

	public void runMatrixFromFile(File file) throws Exception
	{
        Reader reader = CommonHelper.readerFromFile(file);
        Context context = this.factory.createContext();
        Matrix matrix = (Matrix) context.getFactory().createDocument(DocumentKind.MATRIX, file.getPath());
        matrix.load(reader);
        matrix.start(new Date(), null);
	}

	public void stopMatrix(Document document) throws Exception
	{
		if (document instanceof MatrixFx)
		{
			((MatrixFx) document).stop();
		}
	}

	public void startMatrix(Document document) throws Exception
	{
		if (document instanceof MatrixFx)
		{
			((MatrixFx) document).startMatrix();
		}
	}

	public void addToToolbar(String fullPath, String visibleName) throws Exception
	{
		if (!this.toolbarMatrices.contains(fullPath))
		{
			this.controller.addToToolbar(fullPath, visibleName);
			this.toolbarMatrices.add(fullPath);
			this.settings.setValue(Settings.MAIN_NS, Settings.MATRIX_TOOLBAR, new File(fullPath).getPath(), visibleName);
			this.settings.saveIfNeeded();
		}
		else
		{
			DialogsHelper.showInfo(String.format(R.MAIN_ADD_TO_TOOLBAR.get(), fullPath));
		}
	}

	public void removeFromToolbar(String fullPath) throws Exception
	{
		this.toolbarMatrices.remove(fullPath);
		this.settings.remove(Settings.MAIN_NS, Settings.MATRIX_TOOLBAR, fullPath);
		this.settings.saveIfNeeded();
	}

	public void renameFromToolbar(String fullPath, String newName) throws Exception
	{
		this.settings.setValue(Settings.MAIN_NS, Settings.MATRIX_TOOLBAR, fullPath, newName);
		this.settings.saveIfNeeded();
	}

	//endregion

	public CustomTab createCustomTab(Document document)
	{
		return this.controller.createTab(document);
	}

	public void selectTab(CustomTab tab)
	{
		this.controller.selectTab(tab);
	}

	public void clearFileLastOpenMatrix() throws Exception
	{
		this.settings.removeAll(Settings.MAIN_NS, DocumentKind.MATRIX.toString());
		this.settings.saveIfNeeded();

		this.controller.clearLastMatrixMenu();
	}

	public boolean closeApplication()
	{
		try
		{
			if (this.config != null)
			{
				if (this.config.canClose())
				{
					this.config.close();
					setConfiguration(null);
					saveDimensionAndPosition();
					this.controller.close();
					return true;
				}
				return false;
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}

		return true;
	}

	public void showCalculator() throws Exception
	{
		checkConfig();
		this.controller.showCalculator(this.config.createEvaluator());
	}

	void search() throws Exception
	{
		checkConfig();
		List<File> vars = new ArrayList<>();
		vars.add(new File(this.config.getVars().get()));
		vars.addAll(convert(this.config.getUserVars()));

		new Search(this, this.config, this.settings).show();
	}

	private List<File> convert(List<MutableString> list)
	{
		return list.stream().map(MutableString::get).map(File::new).collect(Collectors.toList());
	}

	public void store() throws Exception
	{
		new StoreVariable(this.config).show();
	}

	public void showIntoConfiguration(File file)
	{
		if (!file.getAbsolutePath().contains(new File("").getAbsolutePath()))
		{
			DialogsHelper.showInfo(R.MAIN_SHOW_INFO_CONFIG.get());
			return;
		}
		if (this.config instanceof ConfigurationFx)
		{
			((ConfigurationFx) this.config).scrollToFile(file);
		}
	}

	public void showWaits(boolean flag)
	{
		this.showWaits = flag;
	}

	public boolean isShowWaits()
	{
		return this.showWaits;
	}

	//region Files
	public void createFolder(File parentFolder, String folderName)
	{
		new File(parentFolder.getAbsolutePath() + File.separator + folderName).mkdir();
	}

	public void createFile(File parentFolder, String fileName) throws Exception
	{
		new File(parentFolder.getAbsolutePath() + File.separator + fileName).createNewFile();
	}
	//endregion

	//region private methods
	private void checkConfig() throws Exception
	{
		if (this.config == null)
		{
			throw new EmptyConfigurationException(R.EMPTY_CONFIGURATION_EXCEPTION.get());
		}
	}

	private String newName(Class<? extends Document> clazz) throws Exception
	{
		this.checkConfig();
		DocumentInfo annotation = clazz.getAnnotation(DocumentInfo.class);
		if (annotation == null)
		{
			throw new Exception(String.format(R.MAIN_UNKNOWN_TYPE_OF_DOC.get(),"" + clazz));
		}

		return checkName(annotation.newName());
	}

	private String checkName(String name)
	{
		List<String> names = this.config.getSubordinates().stream().map(e -> e.getNameProperty().get()).collect(Collectors.toList());

		String temp = name;
		int index = 0;
		while (names.contains(name))
		{
			name = temp + "(" + index++ + ")";
		}
		return name;
	}

	private Optional<File> chooseFile(Class<? extends Document> clazz, String filePath, OpenSaveMode mode) throws Exception
	{
		File file;
		if (Str.IsNullOrEmpty(filePath))
		{
			DocumentInfo annotation = clazz.getAnnotation(DocumentInfo.class);
			if (annotation == null)
			{
				throw new Exception(String.format(R.MAIN_UNKNOWN_TYPE_OF_DOC.get(),"" + clazz));
			}
	
			String title		= String.format(R.MAIN_CHOOSE_FILE_TITLE.get(), annotation.description());
			String filter		= String.format(R.MAIN_CHOOSE_FILE_FILTER.get(), annotation.extension(), annotation.extension());
			boolean manyExtensions = annotation.extension().contains(",");
			String extension	= manyExtensions ? annotation.extension() : String.format("*.%s", annotation.extension());
	
			file = DialogsHelper.showOpenSaveDialog(title, filter, extension, mode);
		}
		else
		{
			file = new File(filePath);
		}
		return this.controller.checkFile(file) ? Optional.empty() : Optional.ofNullable(file);
	}

    private Document loadDocument(File file, DocumentKind kind) throws Exception
    {
		if (!file.exists())
		{
			DialogsHelper.showError(String.format(R.MAIN_LOAD_DOC_NOT_FOUND.get(), file.getAbsoluteFile()));
			throw new FileNotFoundException();
		}

        Document doc = this.factory.createDocument(kind, file.getPath());
        
        if (doc == null)
        {
            return null;
        }

        try
        {
            try (Reader reader = CommonHelper.readerFromFile(file))
            {
                doc.load(reader);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
                DialogsHelper.showError(e.getMessage());

                doc = this.factory.createDocument(DocumentKind.PLAIN_TEXT, doc.getNameProperty().get());
                try (Reader reader = CommonHelper.readerFromFile(file))
                {
                    doc.load(reader);
                }
            }
            if (!isFromInit)
            {
            	if (kind.isUseNewMVP())
            	{
					this.factory.showDocument(doc);
				}
				else
				{
					doc.display();
				}
            }
            doc.saved();
            doc.getNameProperty().fire();
            SettingsValue maxSettings = this.settings.getValueOrDefault(Settings.GLOBAL_NS, Settings.SETTINGS, Settings.MAX_LAST_COUNT);
            int max = Integer.parseInt(maxSettings.getValue());
            this.settings.setValue(Settings.MAIN_NS, kind.toString(), new File(doc.getNameProperty().get()).getName(), max, doc.getNameProperty().get());
            this.settings.saveIfNeeded();
            if (kind == DocumentKind.MATRIX)
            {
                this.controller.updateFileLastMatrix(this.settings.getValues(Settings.MAIN_NS, kind.toString()));
            }
            return doc;
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
			DialogsHelper.showError(String.format(R.MAIN_LOAD_DOC_ERROR_CLASS.get(), doc.getClass().getSimpleName(), "" + file));
        }

        return null;
    }

	public void removeMatrixFromSettings(String key) throws Exception
	{
		this.settings.remove(Settings.MAIN_NS, DocumentKind.MATRIX.name(), key);
		this.settings.saveIfNeeded();
		this.controller.updateFileLastMatrix(this.settings.getValues(Settings.MAIN_NS, DocumentKind.MATRIX.name()));
	}

	@Deprecated
	private void createDocument(Document doc) throws Exception
	{
		if (doc == null)
		{
			return;
		}
		doc.create();
		doc.display();
	}

	private void saveDimensionAndPosition() throws Exception
	{
		if (!needForget)
		{
			Dimension dimension = this.controller.getDimension();
			Point position = this.controller.getPosition();
			String str = dimension.getWidth() + SMALL_SEPARATOR + dimension.getHeight() + SEPARATOR + position.getX() + SMALL_SEPARATOR + position.getY();
			this.factory.getSettings().setValue(Settings.GLOBAL_NS, Settings.SETTINGS, DIMENSION_AND_POSITION, str);
			this.factory.getSettings().saveIfNeeded();
		}
	}

	private void loadDimensionAndPosition(BiConsumer<Double, Double> dimOperator, BiConsumer<Double, Double> posOperator)
	{
		SettingsValue settingsValue = this.factory.getSettings().getValue(Settings.GLOBAL_NS, Settings.SETTINGS, DIMENSION_AND_POSITION);
		if (settingsValue != null && !needForget)
		{
			String value = settingsValue.getValue();
			String[] split = value.split(SEPARATOR);
			String dimension = split[0];
			String position = split[1];

			String[] d = dimension.split(SMALL_SEPARATOR);
			dimOperator.accept(Double.valueOf(d[0]), Double.valueOf(d[1]));

			String[] p = position.split(SMALL_SEPARATOR);
			posOperator.accept(Double.valueOf(p[0]), Double.valueOf(p[1]));
		}
	}

	//region private git methods
	private void storeCredential(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	private void checkCredential()
	{
		if (Str.IsNullOrEmpty(this.username) || Str.IsNullOrEmpty(this.password))
		{
			new CredentialDialog(this::storeCredential).display(this.username, this.password);
		}
	}

	// TODO is it steel needed?
	private Git git() throws Exception
	{
		setSSH();
		Repository localRepo = new FileRepositoryBuilder().findGitDir().build();
		return new Git(localRepo);
	}

	private void setSSH()
	{
		CustomJschConfigSessionFactory jschConfigSessionFactory = new CustomJschConfigSessionFactory(this.settings);
		if (jschConfigSessionFactory.isValid())
		{
			SshSessionFactory.setInstance(jschConfigSessionFactory);
		}
	}

    // TODO is it steel needed?
	private CredentialsProvider getCredentialsProvider()
	{
		checkCredential();
		return new CredentialsProvider()
		{
			@Override
			public boolean isInteractive()
			{
				return true;
			}

			@Override
			public boolean supports(CredentialItem... credentialItems)
			{
				return true;
			}

			@Override
			public boolean get(URIish urIish, CredentialItem... credentialItems) throws UnsupportedCredentialItem
			{
				for (CredentialItem item : credentialItems)
				{
					if (item instanceof CredentialItem.StringType)
					{
						((CredentialItem.StringType) item).setValue(password);
						continue;
					}
				}
				return true;
			}
		};
	}

	public static class CustomJschConfigSessionFactory extends JschConfigSessionFactory
	{
		private final Settings settings;
		private boolean isValid = true;
		private String pathToIdRsa;
		private String pathToKnownHosts;

		public CustomJschConfigSessionFactory(Settings settings)
		{
			this.settings = settings;
			SettingsValue idRsa = this.settings.getValue(Settings.GLOBAL_NS, Settings.GIT, Settings.GIT_SSH_IDENTITY);
			SettingsValue knownHosts = this.settings.getValue(Settings.GLOBAL_NS, Settings.GIT, Settings.GIT_KNOWN_HOST);
			this.isValid = idRsa != null && knownHosts != null;
			if (this.isValid)
			{
				this.pathToIdRsa = idRsa.getValue();
				this.pathToKnownHosts = knownHosts.getValue();
			}
		}

		public boolean isValid()
		{
			return isValid;
		}

		@Override
		protected void configure(OpenSshConfig.Host host, Session session)
		{
			session.setConfig("StrictHostKeyChecking", "yes");
		}

		@Override
		protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException
		{
			JSch jsch = super.getJSch(hc, fs);
			jsch.removeAllIdentity();
			jsch.addIdentity(this.pathToIdRsa);
			jsch.setKnownHosts(this.pathToKnownHosts);
			return jsch;
		}
	}
	//endregion

	//endregion
}
