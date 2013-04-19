import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class Checks
{
	// Making a HashSet<String> to hold all bad commits:
	static HashSet<String>	      badCommits	= new HashSet<String>();
	static {
		badCommits.add("first");
		badCommits.add("second");
		badCommits.add("third");
		badCommits.add("fourth");
		badCommits.add("fifth");
		badCommits.add("sixth");
		badCommits.add("seventh");
		badCommits.add("eighth");
		badCommits.add("ninth");
		badCommits.add("tenth");
		badCommits.add("eleventh");
	}
	
	// The runtime for executing commands
	static Runtime	              runtime	 = Runtime.getRuntime();
	
	// The thread executor used for printing the stdout and stderr of the run commands
	public static ExecutorService	exec;
	
	// The stdout and stderr for this class
	final PrintStream	          out;
	final PrintStream	          err;
	
	// A field to contain the current process to be tested that is running.
	Process	                      currProc;
	// A timer to be used to cut down on thread creation
	private static Timer	      t	         = new Timer(true);
	
	public boolean checkMakeClean(File partDir, String makeName)
	{
		out.println("Checking make clean:");
		// Holding the return value. We still need to clean up the directory
		boolean cleanWorked = true;
		// Checking to make sure they did part 1.
		if (partDir == null || partDir.listFiles() == null)
			return false;
		final AtomicBoolean makeErr = new AtomicBoolean(false);
		// Run make clean in the current directory.
		try {
			Process makeClean = runtime.exec("make clean", null, partDir);
			final Scanner stdout = new Scanner(makeClean.getInputStream());
			final Scanner stderr = new Scanner(makeClean.getErrorStream());
			// Print the stdout of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stdout.hasNextLine()) {
						err.flush();
						out.println(stdout.nextLine());
					}
					stdout.close();
				}
			});
			// If there's anything here, then an error occurred and it did not clean correctly
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						makeErr.set(true);
						out.flush();
						err.println(stderr.nextLine());
					}
					stderr.close();
				}
			});
			// Wait for the command to finish
			int success = makeClean.waitFor();
			if (success != 0)
				cleanWorked = false;
		}
		catch (IOException e) {
			System.err.println("Unable to run make clean in directory: " + partDir);
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			System.err.println("Interrupted while waiting for make clean to finish.");
			e.printStackTrace();
		}
		// Checking to make sure that make clean removed everything it should
		for (File f : partDir.listFiles())
			if (f.getName().endsWith(".o") || f.getName().compareTo(makeName) == 0
			        || f.getName().compareTo("core") == 0 || f.getName().compareTo("a.out") == 0
			        || f.getName().endsWith(".gch")) {
				f.delete();
				cleanWorked = false;
			}
		err.flush();
		out.println();
		if (makeErr.get())
			return false;
		return cleanWorked;
	}
	
	public boolean[] bufferCommand(File partDir, String commandName, File inputFile)
	{
		out.println("Command results:");
		// Check to make sure the directory exists (i.e. they did this part)
		if (partDir == null)
			return new boolean[] {true, true};
		
		// Open up the input file for reading, if it exists
		// Valgrind error booleans
		final AtomicBoolean memErr = new AtomicBoolean(true);
		final AtomicBoolean leakErr = new AtomicBoolean(false);
		int success = 1;
		try {
			out.println("Testing " + commandName + " with input file: " + inputFile);
			Process partProc =
			        runtime.exec("valgrind --leak-check=yes ./" + commandName, null, partDir);
			final Scanner stdout = new Scanner(partProc.getInputStream());
			final Scanner stderr = new Scanner(partProc.getErrorStream());
			// Print the stdout of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stdout.hasNextLine()) {
						err.flush();
						out.println(stdout.nextLine());
					}
					stdout.close();
				}
			});
			// This stderr stream contains the output of valgrind. We can easily check for
			// errors because:
			// (1) "ERROR SUMMARY: 0" not being found means there were memory errors
			// (2) "LEAK SUMMARY:" being found means there were... leaks
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						String line = stderr.nextLine();
						// There was no memory error
						if (line.contains("ERROR SUMMARY: 0"))
							memErr.set(false);
						if (line.contains("LEAK SUMMARY:"))
							leakErr.set(true);
						out.flush();
						err.println(line);
					}
					stderr.close();
				}
			});
			// Pipe the input line to the process
			Scanner in = new Scanner(inputFile);
			PrintWriter stdin = new PrintWriter(partProc.getOutputStream());
			while (in.hasNextLine()) {
				String line = in.nextLine();
				stdin.println(line);
				stdin.flush();
			}
			stdin.close();
			success = partProc.waitFor();
			out.println("Return code: " + success + "\n");
			in.close();
		}
		catch (FileNotFoundException e) {
			System.err.println(inputFile.getPath() + " does not exist.");
			e.printStackTrace();
			return new boolean[] {true, true};
		}
		catch (IOException e) {
			System.err.println("Could not run the specified command.");
			return new boolean[] {true, true};
		}
		catch (InterruptedException e) {
			System.err.println("Interrupted while waiting for process to termingate.");
			e.printStackTrace();
		}
		// return code 126 for valgrind means it cannot find the file specified
		if (success == 126)
			return new boolean[] {true, true};
		return new boolean[] {memErr.get(), leakErr.get()};
	}
	
	/**
	 * @param partDir
	 * @param commandName
	 * @param inputFile
	 * @return <code>{memory error, leak error}</code>
	 */
	public boolean[] testCommand(File partDir, final String commandName, File inputFile, int limit)
	{
		out.println("Command results:");
		// Check to make sure the directory exists (i.e. they did this part)
		if (partDir == null)
			return new boolean[] {true, true};
		
		final File fPartDir = partDir;
		// Open up the input file for reading, if it exists
		// Valgrind error booleans
		final AtomicBoolean memErr = new AtomicBoolean(true);
		final AtomicBoolean leakErr = new AtomicBoolean(false);
		int success = 1;
		if (inputFile != null) {
			try {
				Scanner in = new Scanner(inputFile);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					out.println("Testing " + commandName + " with input: " + line);
					Process partProc =
					        runtime.exec("valgrind --leak-check=yes ./" + commandName, null,
					                partDir);
					System.out.println(Thread.currentThread() + ": started "
					        + commandName.split("\\ ")[0]);
					currProc = partProc;
					final Scanner stdout = new Scanner(partProc.getInputStream());
					final Scanner stderr = new Scanner(partProc.getErrorStream());
					// Print the stdout of this process
					exec.execute(new Runnable() {
						@Override
						public void run()
						{
							while (stdout.hasNextLine()) {
								err.flush();
								out.println(stdout.nextLine());
							}
							stdout.close();
						}
					});
					// This stderr stream contains the output of valgrind. We can easily check for
					// errors because:
					// (1) "ERROR SUMMARY: 0" not being found means there were memory errors
					// (2) "LEAK SUMMARY:" being found means there were... leaks
					exec.execute(new Runnable() {
						@Override
						public void run()
						{
							while (stderr.hasNextLine()) {
								String line = stderr.nextLine();
								// There was no memory error
								if (line.contains("ERROR SUMMARY: 0"))
									memErr.set(false);
								if (line.contains("LEAK SUMMARY:"))
									leakErr.set(true);
								out.flush();
								err.println(line);
							}
							stderr.close();
						}
					});
					// Pipe the input line to the process
					PrintWriter stdin = new PrintWriter(partProc.getOutputStream());
					stdin.println(line);
					stdin.flush();
					stdin.close();
					// This process is limited to the given number of seconds
					final Thread waiting = Thread.currentThread();
					if (limit > 0) {
						TimerTask tt = new TimerTask() {
							@Override
							public void run()
							{
								killProcess(fPartDir);
								System.err.println(waiting + ": killed "
								        + commandName.split("\\ ")[0]);
								out.println("Killed " + commandName.split("\\ ")[0]
								        + ". Rerun to see if it behaves itself.");
								out.flush();
							}
						};
						t.schedule(tt, limit * 1000);
						System.out.println(Thread.currentThread() + ": timed waiting on "
						        + commandName.split("\\ ")[0]);
						success = partProc.waitFor();
						// We no longer need to kill that process.
						tt.cancel();
					}
					else {
						System.out.println(Thread.currentThread() + ": waiting on "
						        + commandName.split("\\ ")[0]);
						success = partProc.waitFor();
					}
					out.println("Return code: " + success + "\n");
				}
				in.close();
			}
			catch (FileNotFoundException e) {
				System.err.println(inputFile.getPath() + " does not exist.");
				e.printStackTrace();
				currProc = null;
				return new boolean[] {true, true};
			}
			catch (IOException e) {
				System.err.println("Could not run the specified command.");
				currProc = null;
				return new boolean[] {true, true};
			}
			catch (InterruptedException e) {
				System.err.println("Interrupted while waiting for process to termingate.");
				e.printStackTrace();
			}
		}
		// Otherwise, simply run the specified command
		else {
			try {
				out.println("Testing " + commandName);
				Process partProc =
				        runtime.exec("valgrind --leak-check=yes ./" + commandName, null, partDir);
				System.out.println(Thread.currentThread() + ": started "
				        + commandName.split("\\ ")[0]);
				currProc = partProc;
				final Scanner stdout = new Scanner(partProc.getInputStream());
				final Scanner stderr = new Scanner(partProc.getErrorStream());
				// Print the stdout of this process
				exec.execute(new Runnable() {
					@Override
					public void run()
					{
						while (stdout.hasNextLine()) {
							err.flush();
							out.println(stdout.nextLine());
						}
						stdout.close();
					}
				});
				// This stderr stream contains the output of valgrind. We can easily check for
				// errors because:
				// (1) "ERROR SUMMARY: 0" not being found means there were memory errors
				// (2) "LEAK SUMMARY:" being found means there were... leaks
				exec.execute(new Runnable() {
					@Override
					public void run()
					{
						while (stderr.hasNextLine()) {
							String line = stderr.nextLine();
							// There was no memory error
							if (line.contains("ERROR SUMMARY: 0"))
								memErr.set(false);
							if (line.contains("LEAK SUMMARY:"))
								leakErr.set(true);
							out.flush();
							err.println(line);
						}
						stderr.close();
					}
				});
				// Check the return of the valgrind process
				// This process is limited to the given number of seconds
				final Thread waiting = Thread.currentThread();
				if (limit > 0) {
					TimerTask tt = new TimerTask() {
						@Override
						public void run()
						{
							killProcess(fPartDir);
							System.err.println(waiting + ": killed " + commandName.split("\\ ")[0]);
							out.println("Killed " + commandName.split("\\ ")[0]
							        + ". Rerun to see if it behaves itself.");
							out.flush();
						}
					};
					t.schedule(tt, limit * 1000);
					System.out.println(Thread.currentThread() + ": timed waiting on "
					        + commandName.split("\\ ")[0]);
					success = partProc.waitFor();
					// We no longer need to kill that process.
					tt.cancel();
				}
				else {
					System.out.println(Thread.currentThread() + ": waiting on "
					        + commandName.split("\\ ")[0]);
					success = partProc.waitFor();
				}
				out.println("Return code: " + success + "\n");
			}
			catch (IOException e) {
				System.err.println("An error occured while trying to run: " + commandName);
				currProc = null;
				return new boolean[] {true, true};
			}
			catch (InterruptedException e) {
				System.err.println("Interrupted while waiting for process to complete.");
				e.printStackTrace();
			}
		}
		currProc = null;
		// return code 126 for valgrind means it cannot find the file specified
		if (success == 126)
			return new boolean[] {true, true};
		return new boolean[] {memErr.get(), leakErr.get()};
	}
	
	public boolean checkMake(File partDir, String makeName)
	{
		out.println("Make results:");
		Process partProc = null;
		if (!partDir.isDirectory())
			return false;
		final AtomicBoolean makeErr = new AtomicBoolean(false);
		try {
			partProc = runtime.exec("make", null, partDir);
			final Scanner stdout = new Scanner(partProc.getInputStream());
			final Scanner stderr = new Scanner(partProc.getErrorStream());
			// Print the stdout of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stdout.hasNextLine()) {
						err.flush();
						out.println(stdout.nextLine());
					}
					stdout.close();
				}
			});
			// If anything prints to here on make, then either gcc or make had an error, in which
			// case, this test is a fail
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						// First, check to make sure the line isn't all whitespace or empty
						String line = stderr.nextLine();
						if (line.trim().isEmpty())
							continue;
						makeErr.set(true);
						out.flush();
						err.println(line);
					}
					stderr.close();
				}
			});
		}
		catch (IOException e) {
			System.err.println("Could not run \"make\" in: " + partDir.getPath());
			e.printStackTrace();
		}
		int goodMake = 1;
		if (partProc != null)
			try {
				goodMake = partProc.waitFor();
			}
			catch (InterruptedException e) {
				System.err.println("Interrupted while waiting for make to compile.");
				e.printStackTrace();
			}
		// Finally, check that the executable exists:
		boolean foundExec = false;
		for (File f : partDir.listFiles()) {
			if (f.getName().compareTo(makeName) == 0) {
				foundExec = true;
				break;
			}
		}
		out.println();
		if (goodMake == 0 && foundExec && !makeErr.get())
			return true;
		return false;
	}
	
	public void shutdown()
	{
		out.flush();
		err.flush();
		out.close(); // The underlying stream is closed, so err is closed by our concern.
	}
	
	public boolean checkGitCommits(File gitNotes)
	{
		out.println("Checking git commits:");
		int goodCommits = 0;
		LinkedList<String> commitList = new LinkedList<String>();
		try {
			Scanner in = new Scanner(gitNotes);
			while (in.hasNextLine()) {
				String line = in.nextLine();
				commitList.add(line);
				// We're rejecting any commit of the form: "ORDINAL COMMIT" (case insensitive)
				boolean commonCommit = false;
				for (String s : badCommits) {
					if (line.toLowerCase().contains(s + " commit")) {
						commonCommit = true;
						break;
					}
				}
				if (commonCommit)
					continue;
				// We're rejecting any commit with only one word
				if (line.split("\\ ").length == 2)
					continue;
				goodCommits++;
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			System.err.println("Failed to read in GIT_PATCH.txt!");
			e.printStackTrace();
		}
		if (goodCommits >= 5) {
			for (String s : commitList)
				out.println(s);
			out.println();
			return true;
		}
		for (String s : commitList)
			err.println(s);
		err.println();
		return false;
	}
	
	public void printMessage(String message, int stream)
	{
		if (stream == 0)
			out.println(message);
		else if (stream == 1)
			err.print(message);
	}
	
	public Checks(File target)
	{
		FileOutputStream fileStream = null;
		try {
			fileStream = new FileOutputStream(target);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		out = new PrintStream(fileStream);
		err = new PrintStream(fileStream);
	}
	
	public boolean[] mdbTest(File partDir, String commandName, File inputFile, String portNum)
	{
		out.println("Command results:");
		// Check to make sure the directory exists (i.e. they did this part)
		if (partDir == null)
			return new boolean[] {true, true};
		final File fPartDir = partDir;
		// Open up the input file for reading, if it exists
		// Valgrind error booleans
		final AtomicBoolean memErr = new AtomicBoolean(true);
		final AtomicBoolean leakErr = new AtomicBoolean(false);
		int success = 1;
		try {
			out.println("Testing " + commandName);
			Process partProc =
			        runtime.exec("valgrind --leak-check=yes ./" + commandName, null, partDir);
			System.out.println(Thread.currentThread() + ": started mdb-lookup-server");
			currProc = partProc;
			final Scanner stdout = new Scanner(partProc.getInputStream());
			final Scanner stderr = new Scanner(partProc.getErrorStream());
			// Print the stdout of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stdout.hasNextLine()) {
						err.flush();
						out.println(stdout.nextLine());
					}
					stdout.close();
				}
			});
			// This stderr stream contains the output of valgrind. We can easily check for
			// errors because:
			// (1) "ERROR SUMMARY: 0" not being found means there were memory errors
			// (2) "LEAK SUMMARY:" being found means there were... leaks
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						String line = stderr.nextLine();
						// There was no memory error
						if (line.contains("ERROR SUMMARY: 0"))
							memErr.set(false);
						if (line.contains("LEAK SUMMARY:"))
							leakErr.set(true);
						out.flush();
						err.println(line);
					}
					stderr.close();
				}
			});
			
			// Make a file to hold the mdb server output
			File mdbfile = new File(partDir.getParentFile(), "mdb.out.txt");
			if (mdbfile.exists())
				mdbfile.delete();
			mdbfile.createNewFile();
			
			// For each line of input, make a new nc process
			Scanner in = new Scanner(inputFile);
			// Pause until the socket is no longer available
			System.out.println(Thread.currentThread() + ": Checking for port " + portNum);
			int count = 0;
			while (available(Integer.parseInt(portNum)) && count < 2000) {
				count++;
				Thread.sleep(25);
			}
			// mdb-lookup-server never took the port
			if (!(count < 2000)) {
				killProcess(fPartDir);
				System.err.println(Thread.currentThread() + ": port not bound");
				out.println("mdb-lookup-server port was never bound!\n"
				        + "Rerun to see memory memory errors and heap summary.");
				out.flush();
				in.close();
				return new boolean[] {true, true};
			}
			
			// Each netcat has 5 seconds to complete, at which point we kill the mdb-lookup-server
			// which should free up the netcat
			System.out.println(Thread.currentThread() + ": Running nc on mdb-lookup-server");
			while (in.hasNextLine()) {
				String line = in.nextLine();
				// Write out the command to file
				File ncfile = new File(partDir.getParentFile(), "nc.sh");
				if (ncfile.exists())
					ncfile.delete();
				ncfile.createNewFile();
				PrintStream ncFileOut = new PrintStream(ncfile);
				ncFileOut.println("echo nc for phrase \\\"" + line + "\\\": >> mdb.out.txt");
				ncFileOut.println("echo " + line + " | nc -q 5 localhost " + portNum
				        + " >> mdb.out.txt && echo >> mdb.out.txt");
				ncFileOut.flush();
				ncFileOut.close();
				
				Process ncproc = runtime.exec("bash nc.sh", null, partDir.getParentFile());
				// Gobble this process's streams, as bash should output the nc stuff to file
				new StreamGobbler(ncproc.getErrorStream());
				new StreamGobbler(ncproc.getInputStream());
				final AtomicBoolean killed = new AtomicBoolean(false);
				final Thread ncWait = Thread.currentThread();
				// Close the process
				try {
					TimerTask tt = new TimerTask() {
						@Override
						public void run()
						{
							killProcess(fPartDir);
							System.err.println(ncWait + ": killed mdb-lookup-server");
							out.println("mdb-lookup-server took more than 5 seconds to send a response.");
							out.flush();
							killed.set(true);
						}
					};
					t.schedule(tt, 5 * 1000);
					ncproc.waitFor();
					// If we reach this point, then we don't need to interrupt
					if (!killed.get())
						tt.cancel();
					else
						break;
				}
				catch (InterruptedException e) {
					in.close();
					killProcess(partDir);
					success = partProc.waitFor();
					out.println("Return code: " + success + "\n");
					e.printStackTrace();
					return new boolean[] {true, true};
				}
			}
			in.close();
			killProcess(partDir);
			success = partProc.waitFor();
			out.println("Return code: " + success + "\n");
		}
		catch (IOException e) {
			System.err.println("An error occured while trying to run: " + commandName);
			killProcess(partDir);
			e.printStackTrace();
			return new boolean[] {true, true};
		}
		catch (InterruptedException e) {
			System.err.println("Interrupted while waiting for process to complete.");
			killProcess(partDir);
			e.printStackTrace();
			return new boolean[] {true, true};
		}
		// return code 126 for valgrind means it cannot find the file specified
		currProc = null;
		if (success == 126)
			return new boolean[] {true, true};
		return new boolean[] {memErr.get(), leakErr.get()};
	}
	
	private void killProcess(File procDir)
	{
		try {
			// Get the PID of the process
			Field f = currProc.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			// Kill the process
			Process kill = runtime.exec("kill -2 " + f.get(currProc), null, procDir);
			kill.waitFor();
		}
		catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean checkFileEquiv(File user, File base)
	{
		final AtomicBoolean equal = new AtomicBoolean(true);
		// First, make sure the user's file exists
		if (!user.isFile())
			return false;
		// Then, sanitize the user file
		File script = sanitize(user);
		script.delete();
		// Then run diff, and print the result to file
		String command = "diff '" + user.getAbsolutePath() + "' '" + base.getAbsolutePath() + "'";
		try {
			script.createNewFile();
			PrintStream diffWriter = new PrintStream(script);
			diffWriter.println(command);
			diffWriter.flush();
			diffWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			Process diffExec = runtime.exec("bash san.sh", null, user.getParentFile());
			final Scanner stdout = new Scanner(diffExec.getInputStream());
			final Scanner stderr = new Scanner(diffExec.getErrorStream());
			// Print the stdout of this process
			out.println("diff on " + user.getName() + ":");
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					// If anything prints here, then they aren't the same
					while (stdout.hasNextLine()) {
						err.flush();
						out.println(stdout.nextLine());
						equal.set(false);
					}
					stdout.close();
				}
			});
			// Print the stderr of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						out.flush();
						err.println(stderr.nextLine());
						equal.set(false);
					}
					stderr.close();
				}
			});
			
			// Wait for it to finish
			diffExec.waitFor();
			out.println();
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		script.delete();
		return equal.get();
	}
	
	private File sanitize(File user)
	{
		// Write the bash script to sanitize the file
		File bash = new File(user.getParent(), "san.sh");
		if (bash.isFile())
			bash.delete();
		PrintStream bashWriter = null;
		try {
			bash.createNewFile();
			bashWriter = new PrintStream(bash);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		// Write the script to file
		bashWriter.println("sed '/^[[:space:]]*$/{:a;$d;N;/\\n[[:space:]]*$/ba}' " + user.getName()
		        + " > " + user.getName() + ".2");
		bashWriter.println("mv " + user.getName() + ".2 " + user.getName());
		bashWriter.flush();
		bashWriter.close();
		
		// Execute the script
		try {
			Process bashExec = runtime.exec("bash san.sh", null, user.getParentFile());
			final Scanner stdout = new Scanner(bashExec.getInputStream());
			final Scanner stderr = new Scanner(bashExec.getErrorStream());
			// Print the stdout of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stdout.hasNextLine()) {
						System.err.flush();
						System.out.println(stdout.nextLine());
					}
					stdout.close();
				}
			});
			// Print the stderr of this process
			exec.execute(new Runnable() {
				@Override
				public void run()
				{
					while (stderr.hasNextLine()) {
						System.out.flush();
						System.err.println(stderr.nextLine());
					}
					stderr.close();
				}
			});
			
			// Wait for it to finish
			bashExec.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// We're done
		return bash;
	}
	
	public static boolean available(int port)
	{
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("Invalid start port: " + port);
		}
		
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		}
		catch (IOException e) {}
		finally {
			if (ds != null) {
				ds.close();
			}
			
			if (ss != null) {
				try {
					ss.close();
				}
				catch (IOException e) {
					/* should not be thrown */
				}
			}
		}
		
		return false;
	}
}
