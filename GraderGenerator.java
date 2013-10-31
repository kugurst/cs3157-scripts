import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
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

	// Scanner to read user input
	private Scanner			in;
	private Boolean			TEST		= true;

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
		Object params[] = ConfigParser.parseConfig(options, partAnswers);
		buildScript((Integer) params[0], (Boolean) params[1], partAnswers);
		System.out.println(options);
	}

	private void partBuilder(HashMap<String, String> answers, int partNum)
	{
		String partName = "part" + partNum;
		boolean accepted = false;
		do {
			System.out.print(partName + ") Executable name: ");
			answers.put("exec", in.nextLine());
			do {
				System.out.print(partName
					+ ") Should this program have a time limit (in seconds) [0]: ");
				answers.put("limit", in.nextLine());
			} while (!isIntegerOrEmpty(answers.get("limit")));
			System.out.print(partName + ") Should this program read input from a file []: ");
			answers.put("input-file", in.nextLine());
			System.out
				.print("Do you want to run scripts/programs at various points during testing [n]: ");
			String divergent = in.nextLine();
			if (!divergent.isEmpty() && possibleYes.contains(divergent)) {
				System.out.print("Script to run before building []: ");
				answers.put("script-before-building", in.nextLine());
				System.out.print("Script to run after building []: ");
				answers.put("script-after-building", in.nextLine());
				System.out.print("Script to run during execution []: ");
				answers.put("script-during-run", in.nextLine());
				System.out.print("Script to run after execution []: ");
				answers.put("script-after-run", in.nextLine());
				System.out.print("Script to run after cleaning []: ");
				answers.put("script-after-cleaning", in.nextLine());
			}
			System.out.print("Enter set of command line arguments []: ");
			answers.put("args", parseArgs(in.nextLine()));
			System.out.print("Which parts does " + partName + " depend on []: ");
			answers.put("dependencies", parseParts(in.nextLine()));
			System.out.print("Would you like to compile and test additional drivers [n]: ");
			divergent = in.nextLine();
			if (!divergent.isEmpty() && possibleYes.contains(divergent)) {
				do {
					System.out.print("What directory contains these files: ");
					answers.put("driver-dir", in.nextLine());
				} while (answers.get("driver-dir").isEmpty());
				do {
					System.out.print("What are the executable names: ");
					answers.put("driver-exec", parseNames(in.nextLine()));
				} while (answers.get("driver-exec").isEmpty());
			}
			accepted = acceptSummary(answers);
		} while (!accepted);
	}

	private boolean acceptSummary(HashMap<String, String> answers)
	{
		System.out.println("====Script summary====");
		for (Map.Entry<String, String> entry : answers.entrySet())
			System.out.println(entry.getKey() + " : " + entry.getValue());
		System.out.println("======================");
		System.out.print("Are you sure these parameters are correct [y]: ");
		return possibleYes.contains(in.nextLine());
	}

	private String parseNames(String nextLine)
	{
		String[] execArr = nextLine.split("(\\ )+|[,;:][,;:\\ ]*");
		String names = Arrays.toString(execArr);
		return names.substring(1, names.length() - 1);
	}

	private String parseParts(String nextLine)
	{
		String parts = "";
		if (nextLine.isEmpty())
			return parts;
		ArrayList<Integer> partsArr = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(nextLine);
		while (m.find())
			partsArr.add(Integer.parseInt(m.group()));
		parts = Arrays.toString(partsArr.toArray(new Integer[partsArr.size()]));
		return parts.substring(1, parts.length() - 1);
	}

	private String parseArgs(String nextLine)
	{
		String[] argsArr = nextLine.split("(\\ )*\\|\\|(\\ )*");
		StringBuilder args = new StringBuilder();
		for (String s : argsArr)
			if (!s.isEmpty())
				args.append(s + "||");
		if (args.length() > 0)
			return args.toString().substring(0, args.length() - 2);
		else
			return "";
	}

	int getParts()
	{
		String line;
		do {
			System.out.print("How many parts: ");
			line = in.nextLine();
		} while (!isInteger(line));
		return Integer.parseInt(line);

	}

	private boolean isInteger(String line)
	{
		if (line.isEmpty())
			return false;
		else
			for (Character c : line.toCharArray())
				if (!Character.isDigit(c))
					return false;
		return true;
	}

	int getThreads()
	{
		int threads = (int) Math.round(Runtime.getRuntime().availableProcessors());
		String line;
		do {
			System.out.print("How many threads [" + (threads / 2) + "]: ");
			line = in.nextLine();
		} while (!isIntegerOrEmpty(line));
		int givenThreads = threads + 1;
		if (!line.isEmpty())
			Integer.parseInt(line);
		if (givenThreads <= threads)
			return givenThreads;
		else
			return threads / 2;
	}

	private boolean isIntegerOrEmpty(String line)
	{
		for (Character c : line.toCharArray())
			if (!Character.isDigit(c))
				return false;
		return true;
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
			// Set the current part directory to here
			gw.println("partDir = new File(student, \"part" + partNum + "\");");
			// Preliminary clean
			gw.println("check.printMessage(\"===Preliminary make clean====\", 1);");
			String execNames = getExecNames(execOps);
			gw.println("check.checkMakeClean(partDir, \"" + execNames + "\");");
			gw.println("check.printMessage(\"=============================\", 1);");
			// Inidicate that we're checking this part
			gw.println("check.printMessage(\"\\n" + execNames + " verification:\", 1);");

			// Pre build script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-before-building"));

			// Build any dependencies before hand
			printDepBuild(gw, partNum, (ArrayList<String>) answer.get("dependencies"), partAnswers);

			// Build
			gw.println("goodMake = check.checkMake(partDir, \"" + execNames + "\");");
			printBuildChecks(gw, execNames);

			// Post build script
			printRunScript(gw, (ArrayList<Object>) answer.get("script-after-building"));

			// Run tests
			printPartTest(gw, (ArrayList<Object>) answer.get("names"),
				(ArrayList<Object>) answer.get("script-during-run"));

			// String args = answer.get("args");
			// if (args.isEmpty()) {
			// gw.println(buildCommand(exec, "", answer.get("input-file"), answer.get("limit"))
			// + "\n" + "if (badProgram[0])\n" + "err.println(student.getName() + \" " + exec
			// + ": memory error-\");\n" + "else\n" + "out.println(student.getName() + \" "
			// + exec + ": memory error+\");\n" + "if (badProgram[1])\n"
			// + "err.println(student.getName() + \" " + exec + ": leak error-\");\n"
			// + "else\n" + "out.println(student.getName() + \" " + exec + ": leak error+\");");
			// script = answer.get("script-during-run");
			// if (script != null && !script.isEmpty()) {
			// gw.println("check.runCommand(partDir, \"" + script + "\", null, 0);");
			// }
			// } else {
			// String[] argsArr = args.split("\\|\\|");
			// int run = 0;
			// for (String arg : argsArr) {
			// gw.println("out.println(\"Test " + (run++) + ":\");");
			// gw.println(buildCommand(exec, arg, answer.get("input-file"),
			// answer.get("limit"))
			// + "\n"
			// + "if (badProgram[0])\n"
			// + "err.println(student.getName() + \" "
			// + exec
			// + ": memory error-\");\n"
			// + "else\n"
			// + "out.println(student.getName() + \" "
			// + exec
			// + ": memory error+\");\n"
			// + "if (badProgram[1])\n"
			// + "err.println(student.getName() + \" "
			// + exec
			// + ": leak error-\");\n"
			// + "else\n"
			// + "out.println(student.getName() + \" "
			// + exec + ": leak error+\");");
			// script = answer.get("script-during-run");
			// if (script != null && !script.isEmpty()) {
			// gw.println("check.runCommand(partDir, \"" + script + "\", null, 0);");
			// }
			// }
			// }
			//
			// // Additional drivers
			// if (answer.get("driver-dir") != null)
			// runDrivers(gw, answer);
			//
			// // Post run script
			// script = answer.get("script-after-run");
			// if (script != null && !script.isEmpty()) {
			// gw.println("check.runCommand(partDir, \"" + script + "\", null, 0);");
			// }
			//
			// // Clean up
			// gw.println("cleanWorked = check.checkMakeClean(partDir, \"" + exec + "\");\n"
			// + "if (cleanWorked)\n" + "out.println(student.getName() + \" " + exec
			// + ": make clean+\");\n" + "else\n" + "err.println(student.getName() + \" " + exec
			// + ": make clean-\");");
			//
			// // Clean up dependencies too
			// if (!dep.isEmpty()) {
			// gw.println("check.printMessage(\"===Cleaning dependencies for part" + partNum
			// + "===\", 1);");
			// String[] depArr = dep.split(",");
			// for (String partDep : depArr) {
			// int num = Integer.parseInt(partDep.trim());
			// gw.println("partDep = new File(student, \"part" + num + "\");");
			// gw.println("check.checkMakeClean(partDep, \""
			// + partAnswers.get(num - 1).get("exec") + "\");");
			// }
			// gw.println("check.printMessage(\"===Dependencies cleaned===\", 1);");
			// }
			//
			// // Post clean script
			// script = answer.get("script-after-cleaning");
			// if (script != null && !script.isEmpty()) {
			// gw.println("check.runCommand(partDir, \"" + script + "\", null, 0);");
			// }
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
	private void printPartTest(PrintWriter gw, ArrayList<Object> execParams,
		ArrayList<Object> runScripts)
	{
		// Go through each executable
		for (Object o : execParams) {
			if (o instanceof String) {
				String exec = (String) o;
				gw.println("badProgram = check.testCommand(partDir, \"" + exec
					+ "\", (File) null, 0, null);\n" + "if (badProgram[0])\n"
					+ "err.println(student.getName() + \" " + exec + ": memory error-\");\n"
					+ "else\n" + "out.println(student.getName() + \" " + exec
					+ ": memory error+\");\n" + "if (badProgram[1])\n"
					+ "err.println(student.getName() + \" " + exec + ": leak error-\");\n"
					+ "else\n" + "out.println(student.getName() + \" " + exec + ": leak error+\");");
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
					gw.print("badProgram = check.testCommand(partDir, \"" + exec);
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
						gw.print("new " + params.get("arg-gen"));
					else
						gw.print("null");
					gw.println(");");

					// Print the checks
					gw.println("if (badProgram[0])\n" + "err.println(student.getName() + \" "
						+ exec + ": memory error-\");\n" + "else\n"
						+ "out.println(student.getName() + \" " + exec + ": memory error+\");\n"
						+ "if (badProgram[1])\n" + "err.println(student.getName() + \" " + exec
						+ ": leak error-\");\n" + "else\n" + "out.println(student.getName() + \" "
						+ exec + ": leak error+\");");
				}
			}
		}
	}

	private void printBuildChecks(PrintWriter gw, String execNames)
	{
		String[] nameArr = execNames.split(",\\ ");
		if (nameArr.length == 1) {
			gw.println("if (goodMake[0] && goodMake[1])\n" + "out.println(student.getName() + \" "
				+ execNames + ": make+\");\n" + "else\n" + "err.println(student.getName() + \" "
				+ execNames + ": make-\");");
		} else {
			gw.println("if (goodMake[0])\n"
				+ "out.println(student.getName() + \" Overall make: make+\");\n" + "else\n"
				+ "err.println(student.getName() + \" Overall make: make-\");");
			int pos = 1;
			for (String name : nameArr) {
				gw.println("if (goodMake[" + (pos++) + "])\n"
					+ "out.println(student.getName() + \" " + name + ": make+\");\n" + "else\n"
					+ "err.println(student.getName() + \" " + name + ": make-\");");
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

	private void runDrivers(PrintWriter gw, LinkedHashMap<String, String> answer)
	{
		// Get the driver directory
		String dirName = answer.get("driver-dir");
		String driverExec = answer.get("driver-exec");
		// Run the driver
		gw.println("check.printMessage(\"====Testing additional driver====\", 1);");
		gw.println("File dest = new File(partDir, \""
			+ dirName
			+ "\");\n"
			+ "if (dest.exists())\n"
			+ "deleteFolder(dest);\n"
			+ "dest.mkdir();\n" // + "symlink(partDir, dest, check);\n"
			+ "File src = new File(student.getParent(), \"" + dirName + "\");\n"
			+ "copyFiles(src, dest);\n" + "goodMake = check.checkMake(dest, \"" + driverExec
			+ "\");\n" + "if (goodMake)\n" + "out.println(student.getName() + \" -DRIVER- "
			+ driverExec + ": make+\");\n" + "else\n"
			+ "err.println(student.getName() + \" -DRIVER- " + driverExec + ": make-\");");
		String[] execNames = driverExec.split(",\\ ");
		for (String exec : execNames)
			gw.println("badProgram = check.testCommand(dest, \"" + exec + "\", null, 0);\n"
				+ "if (badProgram[0])\n" + "err.println(student.getName() + \" -DRIVER- " + exec
				+ ": memory error-\");\n" + "else\n"
				+ "out.println(student.getName() + \" -DRIVER- " + exec + ": memory error+\");\n"
				+ "if (badProgram[1])\n" + "err.println(student.getName() + \" -DRIVER- " + exec
				+ ": leak error-\");\n" + "else\n" + "out.println(student.getName() + \" -DRIVER- "
				+ exec + ": leak error+\");");
		gw.println("cleanWorked = check.checkMakeClean(dest, \"" + driverExec + "\");\n"
			+ "if (cleanWorked)\n" + "out.println(student.getName() + \" -DRIVER- " + driverExec
			+ ": make clean+\");\n" + "else\n" + "err.println(student.getName() + \" -DRIVER- "
			+ driverExec + ": make clean-\");");
		gw.println("check.printMessage(\"=================================\", 1);");
	}

	private String buildCommand(String exec, String args, String inputFile, String limit)
	{
		StringBuilder command =
			new StringBuilder("badProgram = check.testCommand(partDir, \"" + exec);
		if (!args.isEmpty())
			command.append(" " + args + "\",");
		else
			command.append("\",");
		if (!inputFile.isEmpty())
			command.append(" new File(\"" + inputFile + "\"),");
		else
			command.append(" null,");
		if (!limit.isEmpty())
			command.append(" " + limit + ");");
		else
			command.append(" 0);");
		return command.toString();
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
