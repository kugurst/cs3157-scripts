import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Grader
{
	// Making a HashSet<String> to hold all bad commits:
	HashSet<String>	badCommits	= new HashSet<String>();
	{
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
	Runtime	        runtime	   = Runtime.getRuntime();
	
	// The thread executor used for printing the stdout and stderr of the run commands
	ExecutorService	exec	   = Executors.newFixedThreadPool(2);
	
	// The input file for isort
	File	        isin	   = new File("isort.in");
	
	public Grader(String root)
	{
		GitFilter filter = new GitFilter();
		File rootDir = new File(root);
		for (File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				System.out.println("Verifying " + f.getName() + "...");
				
				// GIT commit verification //
				// Get the file called GIT_PATCH.txt from the current project directory
				File gitNotes = f.listFiles(filter)[0];
				boolean goodCommit = checkGitCommits(gitNotes);
				if (goodCommit)
					System.out.println(f.getName() + " GIT+");
				else
					System.err.println(f.getName() + " GIT-");
				// End GIT commit verification //
				
				// isort make verification //
				File isortDir = new File(f, "part1");
				// boolean goodMake = checkMake(isortDir, "isort");
				// if (goodMake)
				// System.out.println(f.getName() + " isort: make+");
				// else
				// System.err.println(f.getName() + " isort: make-");
				// end isort make verification //
				
				// Making sure their make clean actually worked:
				boolean cleanWorked = true;
				if (isortDir.listFiles() != null)
					for (File ff : isortDir.listFiles())
						if (ff.getName().endsWith(".o") || ff.getName().compareTo("isort") == 0) {
							ff.delete();
							cleanWorked = false;
						}
				if (cleanWorked)
					System.out.println(f.getName() + " isort: make clean+");
				else
					System.err.println(f.getName() + " isort: make clean-");
			}
		}
		exec.shutdownNow();
	}
	
	private boolean checkMake(File partDir, String makeName)
	{
		Process partProc = null;
		if (!partDir.isDirectory())
			return false;
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
						System.err.flush();
						System.out.println(stderr.nextLine());
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
		if (goodMake == 0 && foundExec)
			return true;
		return false;
	}
	
	private boolean checkGitCommits(File gitNotes)
	{
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
				System.out.println(s);
			return true;
		}
		for (String s : commitList)
			System.err.println(s);
		return false;
	}
	
	/**
	 * @param args
	 */
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
