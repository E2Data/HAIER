#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP POST request to select one of the execution plans of the Pareto
# frontier calculated by the NSGA-II algorithm of HAIER's optimizer, using a
# local JSON file, assuming HAIER is at localhost:8080.

[[ ! $# -eq 2 ]] && echo -e "Usage:\n\t$0 <hex_jobId> <local_file_name>" && \
	exit 1;
CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

$CURL -v -H "Content-Type: application/json" -X POST -d "@$2" \
	http://localhost:8080/e2data/nsga2/"$1"/plans
