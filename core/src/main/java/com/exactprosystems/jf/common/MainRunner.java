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

package com.exactprosystems.jf.common;

import com.exactprosystems.jf.api.common.DateTime;
import com.exactprosystems.jf.api.common.MatrixConnection;
import com.exactprosystems.jf.api.common.Str;
import com.exactprosystems.jf.api.common.Sys;
import com.exactprosystems.jf.common.documentation.DocumentationBuilder;
import com.exactprosystems.jf.common.report.ReportBuilder;
import com.exactprosystems.jf.common.report.ReportFactory;
import com.exactprosystems.jf.common.report.TexReportFactory;
import com.exactprosystems.jf.common.version.VersionInfo;
import com.exactprosystems.jf.documents.ConsoleDocumentFactory;
import com.exactprosystems.jf.documents.DocumentFactory;
import com.exactprosystems.jf.documents.DocumentKind;
import com.exactprosystems.jf.documents.config.Configuration;
import com.exactprosystems.jf.documents.config.ConfigurationBean;
import com.exactprosystems.jf.documents.config.Context;
import com.exactprosystems.jf.documents.guidic.GuiDictionaryBean;
import com.exactprosystems.jf.documents.matrix.Matrix;
import com.exactprosystems.jf.documents.matrix.parser.items.MatrixItem;
import com.exactprosystems.jf.documents.msgdic.MessageDictionaryBean;
import com.exactprosystems.jf.tool.main.Main;
import com.exactprosystems.jf.tool.main.Preloader;
import com.sun.javafx.application.LauncherImpl;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainRunner
{
	private static String restartFileName = "${JF}/.restart.txt";
	private static int    magicNumber     = 7;

	private static final Logger logger = Logger.getLogger(MainRunner.class);

	static String logFileName = ".log.xml";
	static
	{
		// http://stackoverflow.com/a/19053462
		// non-configured log4j doesn't have Appenders
		boolean loggerConfigured = Logger.getRootLogger().getAllAppenders().hasMoreElements();

		if (!loggerConfigured)
		{
			if (!new File(logFileName).exists())
			{
				try (	BufferedReader reader = new BufferedReader(new InputStreamReader(MainRunner.class.getResourceAsStream(logFileName)));
						 BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName)))
				{
					String line = null;

					while ((line = reader.readLine()) != null)
					{
						writer.append(line);
						writer.newLine();
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.exit(1);
				}
			}

			DOMConfigurator.configure(logFileName);
		}
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args)
	{
		int exitCode = 0;
		try
		{
			logger.info("Tool version: " + VersionInfo.getVersion());
			logger.info("args: " + Arrays.toString(hidePassword(args)));
			
			Option startAtName = OptionBuilder
					.withArgName("time")
					.hasArg()
					.withDescription("Specify start matrix time.")
					.create("time");

			Option inputName = OptionBuilder
					.withArgName("file")
					.hasArg()
					.withDescription("Specify input path name.")
					.create("input");

			Option outputName = OptionBuilder
					.withArgName("file")
					.hasArg()
					.withDescription("Specify output path name.")
					.create("output");

			Option configName = OptionBuilder
					.withArgName("file")
					.hasArg()
					.withDescription("Specify configuration file name.")
					.create("config");

			Option traceLevel = OptionBuilder
					.withArgName("level")
					.hasArg()
					.withDescription("Specify verbose level. " + Arrays.toString(VerboseLevel.values()))
					.create("verbose");

			Option password = OptionBuilder
					.withArgName("password")
					.hasArg()
					.withDescription("Specify the password which will be used when tool works with a git repository. ")
					.create("password");

			Option username = OptionBuilder
					.withArgName("username")
					.hasArg()
					.withDescription("Specify the username which will be used when tool works with a git repository. ")
					.create("username");

			Option saveDocs = OptionBuilder
					.withArgName("dir")
					.hasArg()
					.withDescription("Save the documentation in specified folder.")
					.create("docs");

			Option generalDocs = OptionBuilder
					.withArgName("dir")
					.hasArg()
					.withDescription("Save the general documentation in specified folder.")
					.create("generalDocs");

			Option saveSchema 	= new Option("schema", 	"Save the config schema." );
			Option help 		= new Option("help", 	"Print this message." );
			Option versionOut 	= new Option("version", "Print version only.");
			Option shortPaths 	= new Option("short", 	"Show only short paths in tracing." );
            Option console      = new Option("console",	"Do not open GUI. Batch mode.");
            Option forgetScreenPosition = new Option("forgetScreenPosition", "If this option is mentioned then just don't restore position and size from the previous run.");
            Option child      	= new Option("child",	"NEVER use this option. It is only for internal purpose.");

			Options options = new Options();

			options.addOption(startAtName);
			options.addOption(inputName);
			options.addOption(outputName);
			options.addOption(configName);
			options.addOption(traceLevel);
			options.addOption(password);
			options.addOption(username);
			options.addOption(versionOut);
			options.addOption(saveDocs);
			options.addOption(generalDocs);
            options.addOption(saveSchema);
			options.addOption(help);
			options.addOption(shortPaths);
            options.addOption(console);
            options.addOption(child);
			options.addOption(forgetScreenPosition);

			
			CommandLineParser parser = new GnuParser();
			
			CommandLine line = null;
			
			//---------------------------------------------------------------------------------------------------------------------
			// parsing main options that can lead to exit immediately
			//---------------------------------------------------------------------------------------------------------------------
			try
			{
				// parse the command line arguments
				line = parser.parse(options, args);
			}
			catch (ParseException exp)
			{
				// oops, something went wrong
				System.out.println("Incorrect parameters: " + exp.getMessage());

				printHelp(options);

				System.exit(1);
				return;
			}

			if (line.hasOption(saveSchema.getOpt()))
			{
				saveSchema(ConfigurationBean.class,			"schema_conf.xsd");
				saveSchema(MessageDictionaryBean.class,		"schema_mess.xsd");
				saveSchema(GuiDictionaryBean.class,			"schema_gui.xsd");

				System.exit(0);
			}

			if (line.hasOption(saveDocs.getOpt()))
			{
				String dir = line.getOptionValue(saveDocs.getOpt());
				saveDocs(dir);

				System.exit(0);
			}

			if (line.hasOption(generalDocs.getOpt()))
			{
				String dir = line.getOptionValue(generalDocs.getOpt());
				saveGeneralDocs(dir);

				System.exit(0);
			}

			if (line.hasOption(versionOut.getOpt()))
			{
				printVersion();

				System.exit(0);
			}

			if (line.hasOption(help.getOpt()))
			{
				printHelp(options);
				System.exit(0);
			}

			String verboseString = line.getOptionValue(traceLevel.getOpt());
			VerboseLevel verboseLevel = VerboseLevel.All;
			if (verboseString != null)
			{
				verboseLevel = VerboseLevel.valueOf(verboseString);
			}

			String configString = line.getOptionValue(configName.getOpt());
			
			while (true)
			{
				//---------------------------------------------------------------------------------------------------------------------
				// check if this launch is a result of restarting for changing current directory
				//---------------------------------------------------------------------------------------------------------------------
				configString = getRestartConfig(configString);
				line = rebuidCommandLine(line, options, configName, configString);
				
				//---------------------------------------------------------------------------------------------------------------------
				// check if we need restarting app from another directory
				//---------------------------------------------------------------------------------------------------------------------
				Path newPath = needToChangeDirectory(configString);
				if (newPath != null)
				{
					exitCode = restartProcessInNewDir(newPath, line, child);
					
					if (exitCode == magicNumber)
					{
						continue;
					}
					break;
				}
	
				if (line.hasOption(console.getOpt()))
				{
					runInConsoleMode(line, configString, verboseLevel, startAtName, inputName, outputName, shortPaths);
				}
				else
				{
					configString = runInGuiMode(line, configString, username, password, child, forgetScreenPosition);
					if (!Str.IsNullOrEmpty(configString))
					{
						continue;
					}
				}
				break;
			}
			
		} 
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			System.out.println("Error: " + e.getMessage());
			exitCode = 2;
		}

		System.exit(exitCode);
	}

	public static Path needToChangeDirectory(String fileName)
	{
		if (fileName != null)
		{
			File file = new File(fileName);
			Path cwd = Paths.get("").toAbsolutePath();
			File parentDir = file.getParentFile();
            Sys.jfDir = cwd.toAbsolutePath().toString();
			if (parentDir != null)
			{
				Path newCwd = parentDir.toPath();
				if (!cwd.equals(newCwd))
				{
					return newCwd;
				}
			}
		}

		return null;
	}
	
	public static String makeDirWithSubstitutions(String template)
	{
		if (template == null)
		{
			return null;
		}
		
		String home = ".";
		
		try
		{
			File jarName = new File(MainRunner.class.getProtectionDomain()
					.getCodeSource()
					.getLocation().toURI()
					.getPath());
			
			home = "" + jarName.getParentFile();
		}
		catch (URISyntaxException e)
		{
			logger.error(e.getMessage(), e);
		}
		
		return template.replace("${JF}", home);
	}

	private static void runInConsoleMode(CommandLine line, String configString, VerboseLevel verboseLevel, Option startAtName, Option inputName, Option outputName, Option shortPaths) throws Exception
	{
		printVersion();

		DocumentFactory factory = new ConsoleDocumentFactory(verboseLevel);
		Configuration configuration = (Configuration) factory.createDocument(DocumentKind.CONFIGURATION, configString); 
		factory.setConfiguration(configuration);
		
		if (!Str.IsNullOrEmpty(configString))
		{
			try (BufferedReader reader = new BufferedReader(new FileReader(configString)))
		    {
				configuration.load(reader);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			if (!configuration.isValid())
			{
				System.out.println("Configuration is invalid! See the logs for details.");
				System.exit(2);
			}
		}
		else
		{
			System.out.println("Configuration option is empty.");
			System.exit(2);
		}

		if (!line.hasOption(inputName.getOpt()))
		{
			System.out.println(String.format("Error: need %s parameter.", inputName.getOpt()));
			return;
		}
		
		configuration.refresh();

		String input = line.getOptionValue(inputName.getOpt());
		String outputString = line.getOptionValue(outputName.getOpt());
		if (outputString != null)
		{
			configuration.getReports().set(outputString);
		}

		Date startAt = new Date();
		String timeString = line.getOptionValue(startAtName.getOpt());
		if (timeString != null)
		{
			startAt = DateTime.date(timeString);
		}
		
		boolean error = false;

		File inputFile = new File(input); 
		if (!inputFile.exists())
		{
		    System.out.println(String.format("Error: input file %s does not exist", input));
			error = true;
		}
		
		File outputDir = new File(configuration.getReports().get()); 
		if (!outputDir.exists())
		{
		    System.out.println(String.format("Error: output directory %s does not exist", outputDir.getPath()));
			error = true;
		}
		
		if (error)
		{
			return;
		}
		
		boolean showShortPaths = line.hasOption(shortPaths.getOpt()); 
		boolean allPassed = processMatrix(factory, inputFile, startAt, showShortPaths);
		System.exit(allPassed ? 0 : 1);
	}

	private static String runInGuiMode(CommandLine line, String configString, Option username, Option password, Option child, Option forgetScreenResolution)
	{
		String passwordValue = line.getOptionValue(password.getOpt());
		String usernameValue = line.getOptionValue(username.getOpt());
		Main.needForget =  line.hasOption(forgetScreenResolution.getOpt());
		String[] guiArgs = configString != null ? new String[]{ configString, usernameValue, passwordValue } : new String[]{};
		LauncherImpl.launchApplication(Main.class, Preloader.class, guiArgs);
		String config = Main.getConfigName();
		if (config != null && line.hasOption(child.getOpt()))
		{
			createRestartConfig(config);
			System.exit(magicNumber);
		}

		return config;
	}
	
	private static void printHelp(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(
				"java -jar " + Configuration.projectName.toLowerCase() + ".jar",
				"Options", options, 
				"Exit code:\n"
				+ "   0 : all testcases have passed\n"
				+ "   1 : one or more testcases have failed\n"
				+ "   2 : global fault\n", 
				true);
	}

	private static void printVersion()
	{
		System.out.println(Configuration.projectName + "  ver." + VersionInfo.getVersion());
	}

	private static boolean processMatrix(DocumentFactory factory, File matrix, Date startAt, boolean showShortPaths)
	{
		try
		{
			logger.info(String.format("Processing '%s' start at '%s'", matrix.getName(), startAt.toString()));

			try (Reader reader = CommonHelper.readerFromFile(matrix))
			{
				Matrix doc = (Matrix) factory.createDocument(DocumentKind.MATRIX, matrix.getPath());
				doc.load(reader);

				MatrixConnection connection = doc.start(startAt, null);
				connection.join(0);
				return connection.failed() == 0;
			}
			catch (Exception e)
			{
				System.out.println(String.format("Error in matrix '%s' : %s", matrix.getName(), e.getMessage()));
				logger.error(e.getMessage(), e);
			}
		} 
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return false;
	}
	
	private static CommandLine rebuidCommandLine(CommandLine line, Options options, Option configName, String configString) throws ParseException
	{
		List<String> arguments = new ArrayList<>();
		for (Option option : line.getOptions())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("-").append(option.getOpt());
			
			if (option.hasArg())
			{
				String value = line.getOptionValue(option.getOpt());
				sb.append("=").append(option.equals(configName) ? new File(configString).getName() : value);
			}
			arguments.add(sb.toString());
		}
		
		if (!line.hasOption(configName.getOpt()) && !Str.IsNullOrEmpty(configString))
		{
			arguments.add("-" + configName.getOpt() + "=" + new File(configString).getName());
		}
		
		return new GnuParser().parse(options, arguments.toArray(new String[] {}));
	}

	private static int restartProcessInNewDir(Path workDir, CommandLine line, Option child) throws Exception
	{
		List<String> args = new ArrayList<>();
		for (Option option : line.getOptions())
		{
			String arg = "-" + option.getOpt();
			if (option.hasArg())
			{
				arg += "=";
				arg += line.getOptionValue(option.getOpt());
			}
			args.add(arg);
		}
		args.add("-" + child.getOpt());
		
		String fileSeparator 	= System.getProperty("file.separator");
		String javaRuntime  	= System.getProperty("java.home") + fileSeparator + "bin" + fileSeparator + "java";
	
		File jarName = new File(Main.class.getProtectionDomain()
				.getCodeSource()
				.getLocation().toURI()
				.getPath());

		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmParameters = runtimeMxBean.getInputArguments();

		// compose all command-line parameters to launch another process
		List<String> commandLine = new ArrayList<>();
		add(commandLine, javaRuntime);
		
		if (jvmParameters != null)
		{
			for (String param : jvmParameters)
			{
				add(commandLine, param);
			}
		}
		
		if (jarName.isFile())
		{
			add(commandLine, "-jar");
			add(commandLine, jarName.getAbsolutePath());
		}
		else
		{
			add(commandLine, "-cp");
			add(commandLine, jarName.getAbsolutePath());
			add(commandLine, Main.class.getCanonicalName());
		}

		for (String arg : args)
		{
			add(commandLine, arg);
		}

		// launch the process
		ProcessBuilder builder = new ProcessBuilder(commandLine);
		builder
			.redirectInput(Redirect.INHERIT)
		    .redirectOutput(Redirect.INHERIT)
		    .redirectError(Redirect.INHERIT)
			.directory(workDir.toFile());
		
		
		Process process = builder.start();
		process.waitFor();
		
		return process.exitValue();
	}
	
	private static void add(List<String> list, String str)
	{
		if (str != null)
		{
			list.add(str);
		}
	}

	private static void saveDocs(String dir) throws Exception
	{
		DocumentFactory factory = new ConsoleDocumentFactory(VerboseLevel.Errors);
		Configuration configuration = (Configuration) factory.createDocument(DocumentKind.CONFIGURATION, null);
		factory.setConfiguration(configuration);
		Context context = factory.createContext();
		ReportFactory reportFactory = new TexReportFactory();
		if (!Paths.get(dir).toFile().exists())
		{
			Files.createDirectories(Paths.get(dir));
		}
		ReportBuilder report = reportFactory.createReportBuilder(dir, "UserManual_v." + VersionInfo.getVersion() + ".tex", new Date());
		MatrixItem help = DocumentationBuilder.createUserManual(report, context);
		report.reportStarted(null, VersionInfo.getVersion());
		help.execute(context, context.getMatrixListener(), context.getEvaluator(), report);
		report.reportFinished(0, 0, null, null);
		System.out.println("Documentation has been created.");
	}

	private static void saveGeneralDocs(String dir) throws Exception
	{
		DocumentFactory factory = new ConsoleDocumentFactory(VerboseLevel.Errors);
		Configuration configuration = (Configuration) factory.createDocument(DocumentKind.CONFIGURATION, null);
		factory.setConfiguration(configuration);
		Context context = factory.createContext();
		ReportFactory reportFactory = new TexReportFactory();
		if (!Paths.get(dir).toFile().exists())
		{
			Files.createDirectories(Paths.get(dir));
		}
		ReportBuilder report = reportFactory.createReportBuilder(dir, "Exactpro_JackFish-Testing-Tool_General-Information.tex", new Date());
		MatrixItem help = DocumentationBuilder.createGeneralManual(report, context);
		report.reportStarted(null, VersionInfo.getVersion());
		help.execute(context, context.getMatrixListener(), context.getEvaluator(), report);
		report.reportFinished(0, 0, null, null);
		System.out.println("General documentation has been created.");
	}

	private static void saveSchema(Class<?> clazz, final String fileName)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{clazz});

			SchemaOutputResolver sor = new SchemaOutputResolver()
			{
				@Override
				public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException
				{
					System.out.println(fileName);

					File file = new File(fileName);

					StreamResult result = new StreamResult(file);
					result.setSystemId(file.toURI().toURL().toString());
					return result;
				}
			};

			try
			{
				jaxbContext.generateSchema(sor);
			}
			catch (IOException e)
			{
				logger.error(e.getMessage(), e);
			}
		}
		catch (JAXBException e)
		{
			logger.error(e.getMessage(), e);
		}
	}

	private static String getRestartConfig(String init)
	{
		String res = null;
		File file = new File(makeDirWithSubstitutions(restartFileName));
		if (file.exists())
		{
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))) ) 
			{
				res = br.readLine();
			}
			catch (IOException e)
			{
				logger.error(e.getMessage(), e);
			}
			file.delete();
		}
		
		return Str.IsNullOrEmpty(res) ? init : res;
	}
	
	private static void createRestartConfig(String name)
	{
		try (FileWriter fw = new FileWriter(makeDirWithSubstitutions(restartFileName)))
		{
			fw.write(name + '\n');
		}
		catch (IOException e)
		{
			logger.error(e.getMessage(), e);
		}
	}

	private static String[] hidePassword(String[] args)
	{
		String[] a = new String[args.length];
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			a[i] = arg.startsWith("-password") ? "-password=*****" : arg;
		}
		return a;
	}
}
