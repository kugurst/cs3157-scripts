MTGSG: Multi-Threaded Grading Script Generator
==============================================

What Is It?
-----------

This is a grading script generator, written in Java, that takes a specification for a lab, written in YAML, and outputs a file, written for Java, that will grade assignments for the given specification. To be more accurate, it runs the commands, and outputs a summary of those commands to one file, and outputs the actual output of the commands to another file. This behavior can be augmented by using the various parameters detailed in the YAML configuration file.

How Do I Use and Run It?
------------------------

First, you have to write a lab specification. A sample specification, detailing all the possible parameters and their behavior, has been written in `sample-configs/lab-template.yaml`. Make your own configuration (or suggest new parameters!) and when you're done, just run:
```bash
$ make yaml=labN.yaml grader
```
This will create a file in the current directory called `Grader.java`, which will actually grade the specified lab. It should be placed in the same directory with all the student labs. To faciliate the process, I made a simple script in the `tools`, called `symlinker.sh` that will symlink all required file for `Grader.java`, as well as `Grader.java` itself, to whatever directory you run it in. Likewise, it will also remove all the files symlink'ed if you give it the argument `remove`. Note that this will remove all symlinks in the current directory, but only symlinks. Take a look at `tools/symlinker.sh` for implementation details.

Now that you have Grader.java and all the other files in the same location, compile it with:
```bash
$ ~/grading/grading-scripts/mark-java-grading/jdk1.7.0_45/bin/javac Grader.java
```
And run it with:
```bash
$ ~/grading/grading-scripts/mark-java-grading/jdk1.7.0_45/bin/java Grader
```
*Grader requires Java 1.7 functionality, but the CLIC computers installed Java version is 1.6, thus the need to specify the path to the Java 1.7 JDK.*

Grader.java requires no user interaction. After it is done, there will be two files in each student's folder.

Where Are the Results?
----------------------

The results of each student are located at the root of their directory. The
result consists of two files:

### SUMMARY.txt
Contains a simple "+" or "-" to whether or not a student completed the basic tests as specified by the commands that `Check` ran (and interpreted by `Grader`. Take a look at some of the older commits of `GraderMT` for a clearer picture). For example, `checkGitCommits` verifies if a student had at least 5 meaningful commits. `Grader` takes the return of `checkGitCommits` and outputs a simple summary to `SUMMARY.txt`, while `checkGitCommits` outputted the actual commits to `GRADE_RESULT.txt`. *For memory errors and memory leaks, a "+" indicates that there were none.*

### GRADE_RESULT.txt
Contains the output of each of the commands ran. Thus, you'll never actually need to manually test any of the student's code (provided they named their executables and libraries properly, and placed them in the correct location)! It will test the code for you, and log the standard out and standard error of each process to file.

How Can I Score a Lab?
----------------------

Since we can't know in advance all the requirements for a lab, and the point assignment, it's impossible to write an algorithm to *score* any given lab. Thus, I've provided an interface (well, an abstract class) that will allow you to implement that functionality. An `InputGenerator` has three functions:

- String getNextInput()
- void putNextStdOut(String)
- void putNextStdErr(String)

The first function is used to feed input into the current executable being tested. The last two, as their name implies, are used to send the output of that executable to the `InputGenerator`, so that it can decide what to send next, or to just function solely as a scorer. If you don't like Java, similar functionality is provided by the `simul-run` key, though at present it cannot send input to the current executable being tested.

Things You Can Do
-----------------

- A lot of stuff. Take a look at lab-template.yaml to see how flexible this system is.
 - As it stands now, all labs can be tested and scored using this system (provided a robust `InputGenerator`).

To Do
-----

- Finish cleaning up the code in `Checks.java`.
- Provide more user options.
 - Allow controlling of output level.
 - Make `Grader.java` Java 1.6 compatible.
 - Add git commit regex checking to the frontend.
 - Allow input generators to be separate processes.

