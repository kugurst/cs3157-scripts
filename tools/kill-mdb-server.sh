#!/bin/bash
htmlDir=~/www.freewebsitetemplates.com
pid=$(cat mdb-pid.txt)
kill -s SIGTERM $pid
rm -rf mdb-pid.txt mdb-port.txt "$htmlDir"

