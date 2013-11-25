#!/bin/bash
htmlDir=~/www.freewebsitetemplates.com/preview/hairstylesalon
cp -r "$htmlDir" ./
progDir=.
port=$(java -cp ".:$progDir" PortGenLab7)
"$progDir/mdb-lookup-server" "$progDir/mdb-cs3157-www" $port &
pid=$!
echo $pid > mdb-pid.txt
echo $port > mdb-port.txt

