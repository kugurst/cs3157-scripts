#!/bin/bash

# the directory containing the submissions
mbox_dir=$1
# the directory to contain the students work
uni_dir=$2
# the current lab
lab=$3
# the command to call in each student's directory
cmd=$4
# whether to delete or not
del=${5:-$4}
# argument checking
if [ -z "$mbox_dir" ] || [ -z "$uni_dir" ] || [ -z "$lab" ]
then
    echo Usage: mboxer 'mbox_dir' 'dest_dir' 'lab#' '[\"command\"]' '[-d]'
    exit
fi
# checks for the existence of these directories
if [ ! -d "$mbox_dir" ]
then
    echo $mbox_dir is not a directory'!'
    exit
elif [ ! -d "$uni_dir" ]
then
    mkdir -p $uni_dir
    if [ ! -d "$uni_dir" ]
    then
        echo $uni_dir is not a directory and could not be created'!'
        exit
   fi
fi
# mark the working directory to come back to it
cur_dir=$(readlink -f ./)
# expand each file in the mbox dir
for file in $mbox_dir/*.mbox
do
    # get just the name of the mbox file
    file_name=${file##*/}
    # now get just the uni
    uni=${file_name//-*/}
    # Delete the uni dir if it exists
    rm -rf $uni_dir/$uni
    # get the skeleton code of the current lab
    git clone /home/jae/cs3157-pub/$lab $uni_dir/$uni
    # mark the absolute path to the mbox file. We will be changing directories
    # and need to use this file
    mbox_file=$(readlink -f $file)
    # cd to the directory we just cloned to
    cd $uni_dir/$uni
    # expand the student's .mbox file
    git am --whitespace=nowarn $mbox_file > GIT_PATCH.txt
    # display the results to console
    cat GIT_PATCH.txt
    # if there is a command to call, call it
    if [ ! -z "$cmd" ] && [ "$cmd" != '-d' ]
    then
        $cmd
    fi
    # go back to our starting dir
    cd $cur_dir
    # if the user wants us to delete the dir, delete it
    if [ ! -z "$del" ]
    then
        if [ "$del" = '-d' ]
        then
            rm -rf $uni_dir/$uni
        fi
    fi
done
