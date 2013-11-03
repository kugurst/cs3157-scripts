#!/bin/bash
unset CDPATH

# Get directory of script. Method taken from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Get current directory
CURDIR="$PWD"

if [ "$1" == "remove" ]
then
	for file in $CURDIR/*
	do
		if [ -h "$file" ]
		then
			rm "$file"
		fi
	done
else
	ln -s "$DIR/../Checks.java" "$DIR/../ConfigParser.java" "$DIR/../Grader.java" "$DIR/../StreamGobbler.java" "./"
	for file in $DIR/../*Generator.java
	do
		if [ "${file/GraderGenerator}" == "$file" ]
		then
			ln -s "$file"
		fi
	done
fi

