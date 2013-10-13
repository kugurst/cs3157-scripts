import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GraderMT
{
	// The input file for isort
	/**
	 * This input file contains the array sizes I use for testing isort. Change it to whatever you
	 * named yours, or add more
	 */
	File	      mdbin	    = new File("src/mdb.in");
	GitFilter	  filter	= new GitFilter();
	AtomicInteger	counter	= new AtomicInteger(0);
	
	/** This constructor isn't all that interesting */
	public GraderMT(String root, int threads)
	{
		// The extra set is for threads that take too long to quit
		Checks.exec = Executors.newFixedThreadPool(3 * threads + 3);
		Checks.tmArr = new Timer[threads];
		Timer[] tmArr = Checks.tmArr;
		for (int j = 0; j < tmArr.length; j++)
			tmArr[j] = new Timer(true);
		// We'll be single threaded to populate our list with all folders to check.
		File rootDir = new File(root);
		ConcurrentLinkedQueue<File> uniDirs = new ConcurrentLinkedQueue<File>();
		for (File f : rootDir.listFiles())
			if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().startsWith("lab"))
				uniDirs.add(f);
		// Then, spawn the requested number of threads
		Thread[] workers = new Thread[threads];
		for (int i = 0; i < threads; i++) {
			workers[i] = new Thread(new GraderWorker(uniDirs, i));
			workers[i].start();
		}
		// Wait for them to all finish
		for (Thread worker : workers)
			try {
				worker.join();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		// That's it
		Checks.exec.shutdown();
	}
	
	/** Edit me for success */
	class GraderWorker implements Runnable
	{
		// The output and error stream for messages produced by this class.
		PrintStream		            out;
		PrintStream		            err;
		
		// The worker number to retrieve the correct timer in checks
		int		                    number;
		
		// The folder queue
		ConcurrentLinkedQueue<File>	uniDirs;
		
		public GraderWorker(ConcurrentLinkedQueue<File> queue, int number)
		{
			uniDirs = queue;
			this.number = number;
		}
		
		@Override
		public void run()
		{
			File f = null;
			while ((f = uniDirs.poll()) != null) {
				// The Checker Object for this student. Each student gets their own
				Checks check = null;
				// If grader_mod was run to produce the directories, then f.getName() returns the
				// UNI/username of the student we are currently checking
				System.out.println(Thread.currentThread() + ": Verifying " + f.getName() + "...");
				
				// Redirect System.err and System.out to the results file
				/**
				 * These are the files containing the read out of the class. GRADE_RESULTS.txt
				 * contains the raw output of the commands run. SUMMARY.txt contains the pass/fail
				 * decision of the commands run
				 */
				File results = new File(f, "GRADE_RESULTS.txt");
				try {
					if (results.isFile())
						results.delete();
					results.createNewFile();
					check = new Checks(results, number);
				}
				catch (IOException e) {
					System.err.println("Unable to redirect output to file");
					e.printStackTrace();
				}
				
				// This class uses its own streams for reading and writing.
				File summary = new File(f, "SUMMARY.txt");
				try {
					if (summary.isFile())
						summary.delete();
					summary.createNewFile();
					FileOutputStream summaryStream = new FileOutputStream(summary);
					out = new PrintStream(summaryStream);
					err = new PrintStream(summaryStream);
				}
				catch (IOException e) {
					System.err.println("Unable to redirect output to file.");
					e.printStackTrace();
				}
				
				// GIT commit verification //
				// Get the file called GIT_PATCH.txt from the current project directory
				/** Unfortunately, GIT log verification is still a very human process */
				File gitNotes = f.listFiles(filter)[0];
				boolean goodCommit = check.checkGitCommits(gitNotes);
				if (goodCommit)
					out.println(f.getName() + " GIT+");
				else
					err.println(f.getName() + " GIT-");
				// End GIT commit verification //
				
				/**
				 * You can imagine replacing all instances of isort with whatever the executable for
				 * the first project is called
				 */
				check.printMessage("\nMDB-Lookup-Server verification:", 0);
				// mdb-lookup-server make verification //
				File mdbDir = new File(f, "part1");
				boolean goodMake = check.checkMake(mdbDir, "mdb-lookup-server");
				if (goodMake)
					out.println(f.getName() + " mdb-lookup-server: make+");
				else
					err.println(f.getName() + " mdb-lookup-server: make-");
				// end mdb-lookup-server make verification //
				
				// mdb-lookup-server verification //
				/**
				 * isin is a text file containing the sequence of inputs for the program. You don't
				 * even have to enter anything for scanf() and the like, just type it into a file,
				 * and this program will read it in for you. Note that it runs the program once per
				 * input line. If you want to run one program on the entire input file, use
				 * Checks.bufferCommand (same signature and return)
				 */
				// Choose an open port number
				System.out.println(Thread.currentThread() + ": testing mdb-lookup-server");
				String portNum = "";
				do {
					portNum = Integer.toString(8888 + counter.getAndAdd(1));
				}
				while (!Checks.available(Integer.parseInt(portNum)));
				System.out.println(Thread.currentThread() + ": port " + portNum + " is available");
				boolean[] badMdb =
				        check.mdbTest(mdbDir, "mdb-lookup-server ../../mdb-cs3157 " + portNum,
				                mdbin, portNum);
				// are there memory errors?
				if (badMdb[0])
					err.println(f.getName() + " mdb-lookup-server: memory error-");
				else
					out.println(f.getName() + " mdb-lookup-server: memory error+");
				// are there memory leaks?
				if (badMdb[1])
					err.println(f.getName() + " mdb-lookup-server: leak error-");
				else
					out.println(f.getName() + " mdb-lookup-server: leak error+");
				// end mdb-lookup-server verification //
				
				// mdb-lookup-server make clean verification //
				/** Self explanitory */
				boolean cleanWorked = check.checkMakeClean(mdbDir, "mdb-lookup-server");
				if (cleanWorked)
					out.println(f.getName() + " mdb-lookup-server: make clean+");
				else
					err.println(f.getName() + " mdb-lookup-server: make clean-");
				// end isort make clean verification //
				
				/**
				 * Everything here is the same as before. The only difference is the command
				 * verification
				 */
				check.printMessage("\nHTTP-Client verification:", 0);
				
				// http-client make verification //
				File hcDir = new File(f, "part2");
				goodMake = check.checkMake(hcDir, "http-client");
				if (goodMake)
					out.println(f.getName() + " http-client: make+");
				else
					err.println(f.getName() + " http-client: make-");
				// end http-client make verification //
				
				// http-client verification //
				/**
				 * Here, we run http-client 3 times (each return value consists of 2 booleans). Note
				 * that there is no ./ in front of http-client. I then check to see if any of them
				 * had a memory and/or leak error. If even one run is bad, the whole thing is bad
				 * (that is, if one run had leak errors, then this part gets docked for leak errors)
				 */
				boolean[][] twechoSuccess = new boolean[3][2];
				System.out.println(Thread.currentThread() + ": testing http-client");
				twechoSuccess[0] =
				        check.testCommand(hcDir,
				                "http-client www2.warnerbros.com 80 /spacejam/movie/jam.htm", null,
				                10);
				twechoSuccess[1] =
				        check.testCommand(hcDir,
				                "http-client www.gnu.org 80 /software/make/manual/make.html", null,
				                10);
				twechoSuccess[2] =
				        check.testCommand(hcDir, "http-client www.cplusplus.com 80 /index.html",
				                null, 10);
				boolean memErr = false;
				boolean leakErr = false;
				for (boolean[] errSum : twechoSuccess) {
					if (errSum[0])
						memErr = true;
					if (errSum[1])
						leakErr = true;
				}
				if (memErr)
					err.println(f.getName() + " http-client: memory error-");
				else
					out.println(f.getName() + " http-client: memory error+");
				if (leakErr)
					err.println(f.getName() + " http-client: leak error-");
				else
					out.println(f.getName() + " http-client: leak error+");
				// end http-client verification //
				
				// Check to make sure that the two html files were produced and exist
				boolean[] goodHTMLFiles = new boolean[2];
				goodHTMLFiles[0] =
				        check.checkFileEquiv(new File(hcDir, "jam.htm"), new File(
				                "lab6/sol/solutions/jam.htm"));
				goodHTMLFiles[1] =
				        check.checkFileEquiv(new File(hcDir, "make.html"), new File(
				                "lab6/sol/solutions/make.html"));
				if (goodHTMLFiles[0] && goodHTMLFiles[1])
					out.println(f.getName() + " http-client: same file+");
				else
					err.println(f.getName() + " http-client: same file-");
				// end html file equivalence verification //
				
				// http-client make clean verification //
				/** Self explanatory */
				cleanWorked = check.checkMakeClean(hcDir, "http-client");
				if (cleanWorked)
					out.println(f.getName() + " http-client: make clean+");
				else
					err.println(f.getName() + " http-client: make clean-");
				// end http-client make clean verification //
				
				// Clean up
				check.shutdown();
				// Done
				System.out.println(Thread.currentThread() + ": done.");
			}
		}
	}
	
	public static void main(String[] args)
	{
		int threads = (int) Math.round(Runtime.getRuntime().availableProcessors());
		if (args.length == 1)
			if (Integer.parseInt(args[0]) < threads)
				threads = Integer.parseInt(args[0]);
		try {
			/** The first argument is the folder with all the uni's. */
			new GraderMT("lab6/lab6_grade", threads);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class GitFilter implements FilenameFilter
	{
		@Override
		public boolean accept(File dir, String name)
		{
			if (name.compareTo("GIT_PATCH.txt") == 0)
				return true;
			return false;
		}
	}
}
