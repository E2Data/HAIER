#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP GET request to retrieve a new batch of HAIER's logs, assuming
# HAIER is at localhost:8080.

[[ $# -gt 0 ]] && echo -e "Usage:\n\t$0" && exit 1;
CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

$CURL -v -H "Content-Type: application/json" http://localhost:8080/e2data/logs
