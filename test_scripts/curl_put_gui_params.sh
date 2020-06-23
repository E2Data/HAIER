#!/usr/bin/env bash
#
# author: ckatsak
#
# Issue a HTTP PUT request to configure the NSGA-II algorithm of HAIER's
# optimizer, using a local JSON file, assuming HAIER is at localhost:8080.

[[ -z $1 ]] && echo -e "Usage:\n\t$0 <local_file_name>" && exit 1;
CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

$CURL -v -H "Content-Type: application/json" -X PUT -d "@$1" \
	http://localhost:8080/e2data/nsga2/params
