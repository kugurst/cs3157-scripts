Mark's Multi-Threaded Grading Script
====================================

What Is It?
-----------

This is a Java grading script with ultimate extensibility. It's designed to be
flexible enough to grade any kind of lab, but also powerful enough to almost
completely automate the process. Even a lab like lab 7 can be automated using 
the utilities provided by this script, as well as a little sh know how.

How Do I Use It?
----------------

I must profess, in its current form, it's not very user friendly. To modify this 
script to your needs, you must edit the run method of the `GraderWorker` class, 
located in `GraderMT.java`. `Checks.java` is a utility class, but unlike
`java.lang.Math`, it must be instantiated. Each GraderWorker should have its own
`Checks` object, and only one. The `GraderWorker` class has been heavily
commented to show how to use the commands that `Checks` provides, and `Checks`
will eventually be commented.

How Do I Run It?
----------------

After you're done making your changes, copy `GraderMT.java`, `Checks.java`,
`StreamGobbler.java` to the root directory of students' lab directories. E.g.

```bash
$ ls
GraderMT.java   Checks.java StreamGobbler.java aaa1111  aaa2222 aaa3333
aaa4444 aaa5555...
```

For more information on how to get the student directories into that form, take
a look at `mboxer` (detail to come).

Then, just compile and run `GraderMT`.

Where Are the Results?
----------------------

The results of each student are located at the root of their directory. The
result consists of two files:

### SUMMARY.txt
Contains a simple "yes" or "no" to whether or not a student completed the basic
tests as specified by the commands that `Check` ran. For example,
`checkGitCommits` verifies if a student had at least 5 meaningful commits.
`GraderWorker` takes the return of `checkGitCommits` and outputs a simple
summary to `SUMMARY.txt`, while `checkGitCommits` outputted the actual commits
to:

### GRADE_RESULT.txt
Contains the output of each of the commands ran. Thus, you'll never actually
need to test any of the student's code! It will test the code for you, and log
the standard out and standard error of each process to file. `testCommand` lets
you specify this behavior a bit.

To Do
-----

- Finish cleaning up the code in `Checks.java`
- Write a script to generate a `GraderMT.java` from user responses to questions.
- Make `testCommand` more robust.
- Javadoc-comment the `Checks.java`, correctly.

