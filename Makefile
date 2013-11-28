JFLAGS = -cp .:tools/snakeyaml-1.11.jar
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = GraderGenerator.java

default: classes

classes: $(CLASSES:.java=.class)

grader: default
	java -cp .:tools/snakeyaml-1.11.jar GraderGenerator $(yaml)

clean:
	$(RM) *.class tools/*.class *~ tools/*~
