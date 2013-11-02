import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraderGenerator
{
	// A HashSet to quickly parse a "yes"
	static HashSet<String>	possibleYes	= new HashSet<String>();
	static {
		possibleYes.add("Yes");
		possibleYes.add("yes");
		possibleYes.add("y");
		possibleYes.add("Y");
		possibleYes.add("YES");
		possibleYes.add("");
	}

	private Boolean			TEST		= false;
	Executor				exec		= Executors.newCachedThreadPool();
	Pattern					numberPat	= Pattern.compile("\\d+");

	// This class will ask a series of questions to construct a Grader Script
	public GraderGenerator(String[] args)
	{
		if (args.length != 1) {
			System.out.println("java GraderGenerator <lab.yaml>");
			System.exit(2);
		}

		LinkedList<LinkedHashMap<String, Object>> partAnswers =
			new LinkedList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> options = ConfigParser.loadConfig(new File(args[0]));
		if (options.containsKey("extract-mbox"))
			extractMbox(options.remove("extract-mbox"));
		Object params[] = ConfigParser.parseConfig(options, partAnswers);
		buildScript((Integer) params[0], (Boolean) params[1], partAnswers);
		System.out.println(options);
	}

	@SuppressWarnings ("unchecked")
	private void extractMbox(Object mboxMap)
	{
		LinkedHashMap<String, String> mboxParams = (LinkedHashMap<String, String>) mboxMap;
		File mboxDir = new File(mboxParams.get("mbox-from"));
		// Make the folder if it doesn't exist
		final File destFolder = new File(mboxParams.get("dest"));
		if (destFolder.isFile()) {
			System.err.println("Destination folder is file!");
			System.exit(3);
		}
		if (!destFolder.isDirectory() && !destFolder.mkdirs()) {
			System.err.println("Failed to make destination directory!");
			System.exit(3);
		}
		// A pattern used to retrieve the username of submission
		final Pattern labPat = Pattern.compile("-lab\\d+\\.mbox");
		// Mark the original repo
		final File cloneSource = new File(mboxParams.get("clone-from"));
		// git clone and git am each mbox file, threaded of course (because it takes a while)
		// Make the thread
		Thread[] cloneWorkers = new Thread[Runtime.getRuntime().availableProcessors()];
		final ConcurrentLinkedDeque<File> mboxFiles = new ConcurrentLinkedDeque<File>();
		for (File mboxFile : mboxDir.listFiles())
			if (mboxFile.getName().endsWith(".mbox"))
				mboxFiles.add(mboxFile);
		Runnable r = new Runnable() {
			@Override
			public void run()
			{
				File mboxFile;
				while ((mboxFile = mboxFiles.poll()) != null) {
					String uni = mboxFile.getName();
					// Remove the -labN.mbox part
					Matcher m = labPat.matcher(uni);
					m.find();
					uni = uni.substring(0, m.start());
					// Remove the destination directory if it exists
					File dest = new File(destFolder, uni);
					if (dest.isFile())
						dest.delete();
					else if (dest.isDirectory())
						deleteDirectory(dest);
					// Clone the directory
					try {
						Process gitClone =
							Runtime.getRuntime().exec(
								"git clone " + cloneSource.getAbsolutePath() + " "
									+ dest.getAbsolutePath());
						exec.execute(new StreamGobbler(gitClone.getErrorStream()));
						exec.execute(new StreamGobbler(gitClone.getInputStream()));
						int success = gitClone.waitFor();
						if (success != 0) {
							System.err.println(Thread.currentThread() + ": unable to clone for "
								+ uni);
							continue;
						}
						Process gitAM =
							Runtime.getRuntime().exec(
								"git am --whitespace=nowarn " + mboxFile.getAbsolutePath(), null,
								dest);
						exec.execute(new StreamGobbler(gitAM.getErrorStream()));
						exec.execute(new StreamGobbler(gitAM.getInputStream()));
						success = gitAM.waitFor();
						if (success != 0) {
							System.err.println(Thread.currentThread() + ": unable to patch for "
								+ uni);
							continue;
						}
					} catch (IOException e) {
						System.err.println(Thread.currentThread() + ": unable to clone for " + uni);
						e.printStackTrace();
					} catch (InterruptedException e) {
						System.err.println(Thread.currentThread()
							+ ": interrupted while cloning for " + uni);
						e.printStackTrace();
					}
				}
			}
		};
		// Spawn each thread
		for (int i = 0; i < cloneWorkers.length; i++) {
			cloneWorkers[i] = new Thread(r);
			cloneWorkers[i].start();
		}
		// Wait for them to finish
		for (Thread t : cloneWorkers)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(4);
			}
	}

	private void deleteDirectory(File source)
	{
		File[] contents = source.listFiles();
		for (File f : contents) {
			if (f.getName().equals(".") || f.getName().equals(".."))
				continue;
			else if (f.isDirectory())
				deleteDirectory(f);
			else
				f.delete();
		}
		source.delete();
	}

	@SuppressWarnings ({"unchecked"})
	private void buildScript(int threads, boolean checkGit,
		LinkedList<LinkedHashMap<String, Object>> partAnswers)
	{
		File graderFile;
		if (TEST)
			graderFile = new File("src", "Grader.java");
		else
			graderFile = new File("Grader.java");
		if (graderFile.exists())
			if (!graderFile.delete())
				System.exit(1);
		PrintWriter gw = null;
		try {
			if (!graderFile.createNewFile())
				System.exit(1);
			gw = new PrintWriter(new FileOutputStream(graderFile), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Print out the imports
		gw.println("import java.io.File;\n" + "import java.io.FileOutputStream;\n"
			+ "import java.io.IOException;\n" + "import java.io.PrintStream;\n"
			+ "import java.util.Timer;\n" + "import java.util.concurrent.ConcurrentLinkedQueue;\n"
			+ "import java.util.concurrent.Executors;\n" + "import java.nio.file.CopyOption;\n"
			+ "import java.nio.file.Files;\n" + "import java.nio.file.Path;\n"
			+ "import java.nio.file.StandardCopyOption;\n"
			+ "import java.util.concurrent.atomic.AtomicInteger;");
		// Print out the static, single-threaded portion
		gw.println("public class Grader\n" + "{\n"
			+ "AtomicInteger counter = new AtomicInteger(0);\n"
			+ "public Grader(String root, int threads)\n" + "{\n"
			+ "Checks.exec = Executors.newCachedThreadPool();\n"
			+ "Checks.tmArr = new Timer[threads];\n" + "Timer[] tmArr = Checks.tmArr;\n"
			+ "for (int j = 0; j < tmArr.length; j++)\n" + "tmArr[j] = new Timer(true);\n"
			+ "File rootDir = new File(root);\n"
			+ "ConcurrentLinkedQueue<File> uniDirs = new ConcurrentLinkedQueue<File>();\n"
			+ "for (File f : rootDir.listFiles())\n" + getValidDirectories(partAnswers) + "\n"
			+ "uniDirs.add(f);\n" + "Thread[] workers = new Thread[threads];\n"
			+ "for (int i = 0; i < threads; i++) {\n"
			+ "workers[i] = new Thread(new GraderWorker(uniDirs, i));\n" + "workers[i].start();\n"
			+ "}\n" + "for (Thread worker : workers)\n" + "try {\n" + "worker.join();\n"
			+ "} catch (InterruptedException e) {\n" + "e.printStackTrace();\n" + "}\n"
			+ "Checks.exec.shutdown();\n" + "}");
		// Make sure the thread count is correct
		if (threads > Runtime.getRuntime().availableProcessors() || threads <= 0)
			threads = Runtime.getRuntime().availableProcessors() / 2;
		// Print out the main method
		gw.println("public static void main(String[] args)\n" + "{\n" + "new Grader(\"./\", "
			+ threads + ");\n" + "}");
		// Print out the folder delete method
		gw.println("private void deleteFolder(File source)\n" + "{\n"
			+ "File[] contents = source.listFiles();\n" + "for (File f : contents) {\n"
			+ "if (f.getName().equals(\".\") || f.getName().equals(\"..\"))\n" + "continue;\n"
			+ "if (f.isDirectory())\n" + "deleteFolder(f);\n" + "else\n" + "f.delete();\n" + "}\n"
			+ "source.delete();\n" + "}");
		// Print out the symlink method
		gw.println("public void symlink(File src, File dest, Checks check)\n" + "{\n"
			+ "File[] srcFiles = src.listFiles();\n" + "for (File f : srcFiles) {\n"
			+ "if (f.getName().equals(dest.getName()) || f.getName().equals(\"Makefile\"))\n"
			+ "continue;\n" + "check.jockeyCommand(dest, \"ln -s ../\" + f.getName(), null);\n"
			+ "}\n" + "}");
		// Print out the copy folder method
		gw.println("public void copyFiles(File src, File dest)\n" + "{\n" + "Path from;\n"
			+ "Path to;\n" + "CopyOption[] options =\n"
			+ "new CopyOption[] {StandardCopyOption.REPLACE_EXISTING,\n"
			+ "StandardCopyOption.COPY_ATTRIBUTES};\n" + "File[] srcFiles = src.listFiles();\n"
			+ "for (File f : srcFiles) {\n"
			+ "if (f.getName().equals(\".\") || f.getName().equals(\"..\")) {\n" + "continue;\n"
			+ "} else if (f.isDirectory()) {\n" + "File newDir = new File(dest, f.getName());\n"
			+ "newDir.mkdir();\n" + "copyFiles(f, newDir);\n" + "} else {\n"
			+ "from = f.toPath();\n" + "to = new File(dest, f.getName()).toPath();\n" + "try {\n"
			+ "Files.copy(from, to, options);\n" + "} catch (IOException e) {\n"
			+ "e.printStackTrace();\n" + "}\n" + "}\n" + "}\n" + "}");

		// Now for GraderWorker
		gw.println("class GraderWorker implements Runnable\n"
			+ "{\n"
			+ "PrintStream out;\n"
			+ "PrintStream err;\n"
			+ "int number;\n"
			+ "ConcurrentLinkedQueue<File> uniDirs;\n"
			+ "public GraderWorker(ConcurrentLinkedQueue<File> queue, int number)\n"
			+ "{\n"
			+ "uniDirs = queue;\n"
			+ "this.number = number;\n"
			+ "}\n"
			+ "@Override\n"
			+ "public void run()\n"
			+ "{\n"
			+ "File student = null;\n"
			+ "while ((student = uniDirs.poll()) != null) {\n"
			+ "Checks check = null;\n"
			+ "System.out.println(\"Grader \" + number + \": Verifying \" + student.getName() + \"...\");\n"
			+ "File results = new File(student, \"GRADE_RESULTS.txt\");\n" + "try {\n"
			+ "if (results.isFile())\n" + "results.delete();\n" + "results.createNewFile();\n"
			+ "check = new Checks(results, number);\n" + "} catch (IOException e) {\n"
			+ "System.err.println(\"Unable to redirect output to file\");\n"
			+ "e.printStackTrace();\n" + "}\n"
			+ "File summary = new File(student, \"SUMMARY.txt\");\n" + "try {\n"
			+ "if (summary.isFile())\n" + "summary.delete();\n" + "summary.createNewFile();\n"
			+ "FileOutputStream summaryStream = new FileOutputStream(summary);\n"
			+ "out = new PrintStream(summaryStream);\n" + "err = new PrintStream(summaryStream);\n"
			+ "} catch (IOException e) {\n"
			+ "System.err.println(\"Unable to redirect output to file.\");\n"
			+ "e.printStackTrace();\n" + "}");

		// Checking git commits
		if (checkGit) {
			gw.println("boolean goodCommit = check.checkGitCommits(student);\n"
				+ "if (goodCommit)\n" + "out.println(student.getName() + \" GIT+\");\n" + "else\n"
				+ "err.println(student.getName() + \" GIT-\");");
		}

		// Set any persistent variables
		gw.println("File partDir;");
		gw.println("File partDep;");
		gw.println("boolean[] badProgram;");
		gw.println("boolean cleanWorked;");
		gw.println("boolean[] goodMake;");
		// For each part...
		int partNum = 1;
		ArrayList<Object> execOps;
		for (LinkedHashMap<String, Object> answer : partAnswers) {
			execOps = (ArrayList<Object>) answer.get("names");
			System.out.println(execOps);

			// Additional clean targets
			StringBuilder addCleanTargs = new StringBuilder();
			ArrayList<String> cleanTargs = (ArrayList<String>) answer.get("additional-clean");
			if (cleanTargs != null)
				for (String s : cleanTargs)
					addCleanTargs.append(", " + s);

			// Set the current part directory to here
			gw.println("partDir = new File(student, \"part" + partNum + "\");");

			// Preliminary clean
			String execNames = null;
			if (execOps != null) {
				gw.println("check.printMessage(\"===Preliminary make clean====\", 1);");
				execNames = getExecNames(execOps);
				gw.println("check.checkMakeClean(partDir, \"" + execNames
					+ addCleanTargs.toString() + "\");");
				gw.println("check.printMessage(\"=============================\", 1);");
			}

			String allExecs = getNames(execNames, (ArrayList<Object>) answer.get("no-make"));
			// Inidicate that we're checking this part
			gw.println("check.printMessage(\"\\n" + allExecs + " verification:\", 1);");

			// Pre build script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-before-building"));

			// Build any dependencies before hand
			printDepBuild(gw, partNum, (ArrayList<String>) answer.get("dependencies"), partAnswers);

			// Build
			if (execOps != null) {
				gw.println("goodMake = check.checkMake(partDir, \"" + execNames + "\");");
				printBuildChecks(gw, execNames, false);
			}

			// Post build script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-after-building"));

			// Run tests
			if (execOps != null)
				printCommandTest(gw, execOps, false, false);
			if (answer.containsKey("no-make"))
				printCommandTest(gw, (ArrayList<Object>) answer.get("no-make"), false, true);

			// Additional drivers
			printDrivers(gw, (ArrayList<Object>) answer.get("test-drivers"));

			// Post run script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-after-run"));

			// Clean up
			if (execOps != null)
				printClean(gw, execNames + addCleanTargs.toString(), false);

			// Clean up dependencies too
			printDepClean(gw, partNum, (ArrayList<String>) answer.get("dependencies"), partAnswers);

			// Post clean script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-after-cleaning"));
			partNum++;
		}

		// Announce that we're done
		gw.println("check.shutdown();\n"
			+ "System.out.println(\"Grader \" + number + \": done with \"+student.getName()+\".\");");

		// The final brackets
		gw.println("}\n}\n}\n}");
		// Done
		gw.close();
	}

	@SuppressWarnings ("unchecked")
	private String getNames(String execNames, ArrayList<Object> noMakeList)
	{
		StringBuilder allNames = new StringBuilder();
		if (execNames != null)
			allNames.append(execNames);
		if (noMakeList == null)
			return allNames.toString();
		for (Object o : noMakeList) {
			if (o instanceof String)
				allNames.append(", " + o);
			else
				for (String name : ((LinkedHashMap<String, Object>) o).keySet())
					allNames.append(", " + name);
		}
		if (allNames.substring(0, 2).equals(", "))
			return allNames.toString().substring(2);
		else
			return allNames.toString();
	}

	@SuppressWarnings ("unchecked")
	private void printDepClean(PrintWriter gw, int partNum, ArrayList<String> depList,
		LinkedList<LinkedHashMap<String, Object>> partAnswers)
	{
		if (depList == null)
			return;
		gw.println("check.printMessage(\"===Cleaning dependencies for part" + partNum
			+ "===\", 1);");
		for (String partDep : depList) {
			// Get the number part
			Matcher m = numberPat.matcher(partDep);
			m.find();
			int num = Integer.parseInt(m.group());
			gw.println("partDep = new File(student, \"part" + num + "\");");
			gw.println("check.checkMakeClean(partDep, \""
				+ getExecNames((ArrayList<Object>) partAnswers.get(num - 1).get("names")) + "\");");
		}
		gw.println("check.printMessage(\"===Dependencies cleaned===\", 1);");
	}

	private void printClean(PrintWriter gw, String execNames, boolean driver)
	{
		gw.println("cleanWorked = check.checkMakeClean(" + (driver ? "dest" : "partDir") + ", \""
			+ execNames + "\");\n" + "if (cleanWorked)\n" + "out.println(student.getName() + \" "
			+ (driver ? "-DRIVER- " : "") + execNames + ": make clean+\");\n" + "else\n"
			+ "err.println(student.getName() + \" " + (driver ? "-DRIVER- " : "") + execNames
			+ ": make clean-\");");
	}

	@SuppressWarnings ("unchecked")
	private void printDrivers(PrintWriter gw, ArrayList<Object> driverList)
	{
		// For each driver
		if (driverList == null)
			return;
		gw.println("check.printMessage(\"====Testing additional driver====\", 1);");
		for (Object o : driverList) {
			LinkedHashMap<String, Object> driverInfo = (LinkedHashMap<String, Object>) o;
			String dirName = null;
			// There should only be a single key in this map, corresponding to the name of the
			// directory
			for (String s : driverInfo.keySet())
				dirName = s;
			// Get the list of drivers in this directory
			ArrayList<Object> drivers = (ArrayList<Object>) driverInfo.get(dirName);
			StringBuilder driverNameBuilder = new StringBuilder();
			for (Object driver : drivers) {
				if (driver instanceof String)
					driverNameBuilder.append((String) driver + ", ");
				else {
					LinkedHashMap<String, Object> driverParams =
						(LinkedHashMap<String, Object>) driver;
					for (String s : driverParams.keySet())
						driverNameBuilder.append(s + ", ");
				}
			}
			String driverExec = driverNameBuilder.substring(0, driverNameBuilder.length() - 2);

			// Make the drivers for this folder
			gw.println("File dest = new File(partDir, \"" + dirName + "\");\n"
				+ "if (dest.exists())\n"
				+ "deleteFolder(dest);\n"
				+ "dest.mkdir();\n" // + "symlink(partDir, dest, check);\n"
				+ "File src = new File(student.getParent(), \"" + dirName + "\");\n"
				+ "copyFiles(src, dest);\n" + "goodMake = check.checkMake(dest, \"" + driverExec
				+ "\");");
			printBuildChecks(gw, driverExec, true);

			// Run the driver
			printCommandTest(gw, drivers, true, false);

			// Clean up the drivers
			printClean(gw, driverExec, true);
		}
		gw.println("check.printMessage(\"=================================\", 1);");
	}

	@SuppressWarnings ("unchecked")
	private void printCommandTest(PrintWriter gw, ArrayList<Object> execParams, boolean driver,
		boolean noValgrind)
	{
		// Go through each executable
		for (Object o : execParams) {
			if (o instanceof String) {
				String exec = (String) o;
				gw.println("badProgram = check.testCommand(" + (driver ? "dest" : "partDir")
					+ ", \"" + exec + "\", (File) null, 0, null, null, " + noValgrind + ");\n"
					+ "if (badProgram[0])\n" + "err.println(student.getName() + \" "
					+ (driver ? "-DRIVER- " : "") + exec + ": memory error-\");\n" + "else\n"
					+ "out.println(student.getName() + \" " + (driver ? "-DRIVER- " : "") + exec
					+ ": memory error+\");\n" + "if (badProgram[1])\n"
					+ "err.println(student.getName() + \" " + (driver ? "-DRIVER- " : "") + exec
					+ ": leak error-\");\n" + "else\n" + "out.println(student.getName() + \" "
					+ (driver ? "-DRIVER- " : "") + exec + ": leak error+\");");
			} else {
				LinkedHashMap<String, Object> commandParams = (LinkedHashMap<String, Object>) o;
				System.out.println(commandParams);
				String exec = null;
				// This should actually only loop once
				for (String s : commandParams.keySet())
					exec = s;
				LinkedHashMap<String, Object> params =
					(LinkedHashMap<String, Object>) commandParams.get(exec);
				ArrayList<String> args = (ArrayList<String>) params.get("args");
				boolean printRun;
				if (args == null) {
					args = new ArrayList<String>();
					args.add("");
					printRun = false;
				} else
					printRun = args.size() > 1;
				int run = 1;
				for (String arg : args) {
					// Print the run number
					if (printRun)
						gw.println("out.println(\"Test " + (run++) + ":\");");
					// Format the command
					gw.print("badProgram = check.testCommand(" + (driver ? "dest" : "partDir")
						+ ", \"" + exec);
					if (arg.isEmpty())
						gw.print("\", ");
					else
						gw.print(" " + arg + "\", ");

					if (params.containsKey("input-file"))
						gw.print("new File(\"" + params.get("input-file") + "\"), ");
					else if (params.containsKey("input-gen"))
						gw.print("new " + params.get("input-gen") + ", ");
					else
						gw.print("(File) null, ");

					if (params.containsKey("limit"))
						gw.print(params.get("limit") + ", ");
					else
						gw.print("0, ");

					if (params.containsKey("arg-gen"))
						gw.print("new " + params.get("arg-gen") + ", ");
					else
						gw.print("null, ");

					if (params.containsKey("simul-run"))
						gw.print("\"" + params.get("simul-run") + "\"");
					else
						gw.print("null");
					gw.println(", " + noValgrind + ");");

					// Print the checks
					gw.println("if (badProgram[0])\n" + "err.println(student.getName() + \" "
						+ (driver ? "-DRIVER- " : "") + exec + ": memory error-\");\n" + "else\n"
						+ "out.println(student.getName() + \" " + (driver ? "-DRIVER- " : "")
						+ exec + ": memory error+\");\n" + "if (badProgram[1])\n"
						+ "err.println(student.getName() + \" " + (driver ? "-DRIVER- " : "")
						+ exec + ": leak error-\");\n" + "else\n"
						+ "out.println(student.getName() + \" " + (driver ? "-DRIVER- " : "")
						+ exec + ": leak error+\");");
				}
			}
		}
	}

	private void printBuildChecks(PrintWriter gw, String execNames, boolean driver)
	{
		String mark = "";
		if (driver)
			mark = "-DRIVER- ";
		String[] nameArr = execNames.split(",\\ ");
		if (nameArr.length == 1) {
			gw.println("if (goodMake[0] && goodMake[1])\n" + "out.println(student.getName() + \" "
				+ mark + execNames + ": make+\");\n" + "else\n"
				+ "err.println(student.getName() + \" " + mark + execNames + ": make-\");");
		} else {
			gw.println("if (goodMake[0])\n" + "out.println(student.getName() + \" " + mark
				+ "Overall make: make+\");\n" + "else\n" + "err.println(student.getName() + \" "
				+ mark + "Overall make: make-\");");
			int pos = 1;
			for (String name : nameArr) {
				gw.println("if (goodMake[" + (pos++) + "])\n"
					+ "out.println(student.getName() + \" " + mark + name + ": make+\");\n"
					+ "else\n" + "err.println(student.getName() + \" " + mark + name
					+ ": make-\");");
			}
		}
	}

	@SuppressWarnings ("unchecked")
	private void printDepBuild(PrintWriter gw, int partNum, ArrayList<String> depList,
		LinkedList<LinkedHashMap<String, Object>> partAnswers)
	{
		if (depList == null)
			return;
		gw.println("check.printMessage(\"===Building dependencies for part" + partNum
			+ "===\", 1);");
		for (String partDep : depList) {
			// Get the number part
			Matcher m = numberPat.matcher(partDep);
			m.find();
			int num = Integer.parseInt(m.group());
			gw.println("partDep = new File(student, \"part" + num + "\");");
			gw.println("check.checkMake(partDep, \""
				+ getExecNames((ArrayList<Object>) partAnswers.get(num - 1).get("names")) + "\");");
		}
		gw.println("check.printMessage(\"===Dependencies built===\", 1);");
	}

	private void printRunScript(PrintWriter gw, ArrayList<Object> scriptArr)
	{
		System.out.println("PRS: " + scriptArr);
		if (scriptArr == null)
			return;
		for (Object scriptParams : scriptArr)
			if (scriptParams instanceof String)
				gw.println("check.runCommand(partDir, \"" + scriptParams + "\", null, 0);");
	}

	@SuppressWarnings ("unchecked")
	private String getExecNames(ArrayList<Object> execOps)
	{
		StringBuilder execNames = new StringBuilder();
		for (Object execParams : execOps)
			if (execParams instanceof String)
				execNames.append((String) execParams + ", ");
			else
				for (String name : ((LinkedHashMap<String, Object>) execParams).keySet())
					execNames.append(name + ", ");
		return execNames.length() > 0 ? execNames.substring(0, execNames.length() - 2) : execNames
			.toString();
	}

	@SuppressWarnings ("unchecked")
	private String getValidDirectories(LinkedList<LinkedHashMap<String, Object>> answerList)
	{
		StringBuilder dirs =
			new StringBuilder(
				"if (f.isDirectory() && !f.getName().startsWith(\".\") && !f.getName().startsWith(\"lab\")");
		for (LinkedHashMap<String, Object> map : answerList) {
			ArrayList<LinkedHashMap<String, Object>> driverList =
				(ArrayList<LinkedHashMap<String, Object>>) map.get("test-drivers");
			if (driverList != null)
				for (LinkedHashMap<String, Object> driverMap : driverList)
					for (String driverDir : driverMap.keySet())
						dirs.append(" && !f.getName().equalsIgnoreCase(\"" + driverDir + "\")");
		}
		return dirs.toString() + ")";
	}

	public static void main(String[] args)
	{
		new GraderGenerator(args);
	}
}
