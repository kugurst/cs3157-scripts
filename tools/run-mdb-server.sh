#!/bin/bash
progDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
htmlDir="$progDir/../htdocs"
cp -r "$htmlDir" ./htdocs
port=$(java -cp ".:$progDir" PortGenLab7)
"$progDir/mdb-lookup-server" "$progDir/mdb-cs3157-www" $port &
pid=$!
echo $pid > mdb-pid.txt
echo $port > mdb-port.txt

