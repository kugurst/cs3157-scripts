#!/bin/bash
progDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
htmlDir="$progDir/../htdocs"
rm -rf ./htdocs
cp -r "$htmlDir" ./htdocs
port=$(~/grading/grading-scripts/mark-java-grading/jdk1.7.0_45/jre/bin/java -cp ".:$progDir" PortGenLab7)
"$progDir/mdb-lookup-server" "$progDir/mdb-cs3157-www" $port > /dev/null 2>&1 &
pid=$!
sleep 2
echo $pid > mdb-pid.txt
echo $port > mdb-port.txt

