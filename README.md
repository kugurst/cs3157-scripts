Mark's Multi-Threaded Grading Script Generator
==============================================

What Is It?
-----------

This is a Java grading script generator with great extensibility. It's designed to be flexible enough to grade any kind of lab, but also powerful enough to almost completely automate the process. Even a lab like lab 7 can be automated using the utilities provided by this script, as well as a little sh know how.

How Do I Use It?
----------------

Just compile and run `GraderGenerator.java` and answer the questions it poses. Alternatively, you could just feed in a text file containing the answers.

How Do I Run It?
----------------

Once you're done answering the questions, the grading script produces a file called `Grader.java`. This file, along with `Checks.java` and `StreamGobbler.java` should be coppied to the root directory of the students' lab directories. E.g.:

```bash
$ ls
Grader.java   Checks.java StreamGobbler.java aaa1111  aaa2222 aaa3333
aaa4444 aaa5555...
```

Alternatively, you could just symlink `Checks.java` and `StreamGobbler.java`, as they are unlikely to undergo code-breaking changes as compared to `Grader.java` (e.g. someone else runs `GraderGenerator`).

For more information on how to get the student directories into this form, take a look at `mboxer` (details to come).

Then, just compile and run `Grader` (it takes no commandline arguments and requires no user interaction). A note, `Grader` requires Java 7. A distribution of Java 7 is located in the original script directory. To run it, you'll need to run:

```bash
~/grading/grading-scripts/mark-java-grading/jdk1.7.0_45/bin/java Grader
```

Until CLIC updates the Java runtime on the CLIC machines.


Where Are the Results?
----------------------

The results of each student are located at the root of their directory. The
result consists of two files:

### SUMMARY.txt
Contains a simple "yes" or "no" to whether or not a student completed the basic tests as specified by the commands that `Check` ran (and interpreted by `Grader`. Take a look at some of the older commits of `GraderMT` for a clearer picture). For example, `checkGitCommits` verifies if a student had at least 5 meaningful commits. `Grader` takes the return of `checkGitCommits` and outputs a simple summary to `SUMMARY.txt`, while `checkGitCommits` outputted the actual commits to:

### GRADE_RESULT.txt
Contains the output of each of the commands ran. Thus, you'll never actually need to manually test any of the student's code (provided they named their executables and libraries properly)! It will test the code for you, and log the standard out and standard error of each process to file.

Things You Can Do
-----------------

- Run scripts at various points during grading (building, execution, cleaning).
- Automatically kill processes based on a time out.
- Run executables with multiple command line arguments.
- Run test drivers specified in a given folder.
- Regex matching for gitCommits (that is, a match means it's a meaningless commit).
- Make a program read input from a file.
- Part dependencies
- More to come!

To Do
-----

- Finish cleaning up the code in `Checks.java`.
- Pipe process standard out to simultaneously running script.
- Provide more user options.
 - Allow controlling of output level.
 - Create an actual configuration file syntax.
- Make procedures for labs like lab 6 and lab 7.
- Allow multiple executables per part.
- Generate a routine for a scoring script (will take the output of the running program, as well as the check results).
- ETC.

