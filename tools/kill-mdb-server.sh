#!/bin/bash
pid=$(cat mdb-pid.txt)
kill -s SIGTERM $pid
rm -rf mdb-pid.txt mdb-port.txt http-port.txt

