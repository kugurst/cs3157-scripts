import java.io.File;
import java.io.FilenameFilter;

public class Grader
{
	// The input file for isort
	File	isin	= new File("src/isort.in");
	
	public Grader(String root)
	{
		GitFilter filter = new GitFilter();
		File rootDir = new File(root);
		for (File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				// If grader_mod was run to produce the directories, then f.getName() returns the
				// UNI/username of the student we are currently checking
				System.out.println("Verifying " + f.getName() + "...");
				
				// GIT commit verification //
				// Get the file called GIT_PATCH.txt from the current project directory
				File gitNotes = f.listFiles(filter)[0];
				boolean goodCommit = Checks.checkGitCommits(gitNotes);
				if (goodCommit)
					System.out.println(f.getName() + " GIT+");
				else
					System.err.println(f.getName() + " GIT-");
				// End GIT commit verification //
				
				// isort make verification //
				File isortDir = new File(f, "part1");
				boolean goodMake = Checks.checkMake(isortDir, "isort");
				if (goodMake)
					System.out.println(f.getName() + " isort: make+");
				else
					System.err.println(f.getName() + " isort: make-");
				// end isort make verification //
				
				// isort verification //
				boolean goodIsort = Checks.testCommand(isortDir, "isort", isin);
				if (goodIsort)
					System.out.println(f.getName() + "isort: test+");
				else
					System.err.println(f.getName() + "isort: test-");
				// end isort verification //
				
				// isort make clean verification //
				boolean cleanWorked = Checks.checkMakeClean(isortDir, "isort");
				if (cleanWorked)
					System.out.println(f.getName() + " isort: make clean+");
				else
					System.err.println(f.getName() + " isort: make clean-");
				// end isort make clean verification //
				
				// twecho make verification //
				File twDir = new File(f, "part2");
				goodMake = Checks.checkMake(twDir, "twecho");
				if (goodMake)
					System.out.println(f.getName() + " twecho: make+");
				else
					System.err.println(f.getName() + " twecho: make-");
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
					System.out.println(f.getName() + "twecho: test+");
				else
					System.err.println(f.getName() + "twecho: test-");
				// end twecho verification //
				
				// twecho make clean verification //
				cleanWorked = Checks.checkMakeClean(twDir, "twecho");
				if (cleanWorked)
					System.out.println(f.getName() + " twecho: make clean+");
				else
					System.err.println(f.getName() + " twecho: make clean-");
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
