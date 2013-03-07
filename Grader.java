import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;

public class Grader
{
	// The input file for isort
	File	    isin	= new File("src/isort.in");
	
	// The standard error and output streams. Saving them as they will be redirected
	PrintStream	stdout	= System.out;
	PrintStream	stderr	= System.err;
	
	// The output and error stream for messages produced by this class.
	PrintStream	out;
	PrintStream	err;
	
	public Grader(String root)
	{
		GitFilter filter = new GitFilter();
		File rootDir = new File(root);
		for (File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				// If grader_mod was run to produce the directories, then f.getName() returns the
				// UNI/username of the student we are currently checking
				stdout.println("Verifying " + f.getName() + "...");
				
				// Redirect System.err and System.out to the results file
				File results = new File(f, "GRADE_RESULTS.txt");
				try {
					if (results.isFile())
						results.delete();
					results.createNewFile();
					PrintStream resultsStream = new PrintStream(results);
					System.setErr(resultsStream);
					System.setOut(resultsStream);
				}
				catch (IOException e) {
					stderr.println("Unable to redirect output to file");
					e.printStackTrace();
				}
				
				// This class uses its own streams for reading and writing.
				File summary = new File(f, "SUMMARY.txt");
				try {
					if (summary.isFile())
						summary.delete();
					summary.createNewFile();
					PrintStream summaryStream = new PrintStream(summary);
					out = summaryStream;
					err = summaryStream;
				}
				catch (IOException e) {
					stderr.println("Unable to redirect output to file.");
					e.printStackTrace();
				}
				
				// GIT commit verification //
				// Get the file called GIT_PATCH.txt from the current project directory
				File gitNotes = f.listFiles(filter)[0];
				boolean goodCommit = Checks.checkGitCommits(gitNotes);
				if (goodCommit)
					out.println(f.getName() + " GIT+");
				else
					err.println(f.getName() + " GIT-");
				// End GIT commit verification //
				
				System.out.println("Isort verification:");
				// isort make verification //
				File isortDir = new File(f, "part1");
				boolean goodMake = Checks.checkMake(isortDir, "isort");
				if (goodMake)
					out.println(f.getName() + " isort: make+");
				else
					err.println(f.getName() + " isort: make-");
				// end isort make verification //
				
				// isort verification //
				boolean goodIsort = Checks.testCommand(isortDir, "isort", isin);
				if (goodIsort)
					out.println(f.getName() + " isort: test+");
				else
					err.println(f.getName() + " isort: test-");
				// end isort verification //
				
				// isort make clean verification //
				boolean cleanWorked = Checks.checkMakeClean(isortDir, "isort");
				if (cleanWorked)
					out.println(f.getName() + " isort: make clean+");
				else
					err.println(f.getName() + " isort: make clean-");
				// end isort make clean verification //
				
				System.out.println("\nTwecho verification:");
				
				// twecho make verification //
				File twDir = new File(f, "part2");
				goodMake = Checks.checkMake(twDir, "twecho");
				if (goodMake)
					out.println(f.getName() + " twecho: make+");
				else
					err.println(f.getName() + " twecho: make-");
				// end twecho make verification //
				
				// twecho verification //
				boolean[] twechoSuccess = new boolean[3];
				twechoSuccess[0] = Checks.testCommand(twDir, "twecho hello world dude", null);
				twechoSuccess[1] =
				        Checks.testCommand(twDir, "twecho 129!oihd as923!#0 njkdas54%()", null);
				twechoSuccess[2] = Checks.testCommand(twDir, "twecho I AM IN ALL CAPS 1", null);
				boolean goodTwecho = true;
				for (boolean success : twechoSuccess)
					if (!success)
						goodTwecho = false;
				if (goodTwecho)
					out.println(f.getName() + " twecho: test+");
				else
					err.println(f.getName() + " twecho: test-");
				// end twecho verification //
				
				// twecho make clean verification //
				cleanWorked = Checks.checkMakeClean(twDir, "twecho");
				if (cleanWorked)
					out.println(f.getName() + " twecho: make clean+");
				else
					err.println(f.getName() + " twecho: make clean-");
				// end twecho make clean verification //
			}
			break;
		}
		Checks.shutdown();
	}
	
	public static void main(String[] args)
	{
		new Grader("dat");
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
