#!/bin/bash

# Args: configfile

set -e

configFile="$1"

if [ -z "$configFile" ]; then
    echo "Need 1 param: [config_file.json]"
    exit 1
fi

lastRun="`sh scripts/get-last-run.sh $configFile`"

file="`ls $lastRun/results* | tail -n 1`"
grepArg='.*'

cat "$file" | grep "$grepArg"

