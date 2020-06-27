#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP GET request to retrieve the execution plans in the Pareto
# frontier calculated by the NSGA-II algorithm of HAIER's optimizer, assuming
# HAIER is at localhost:8080.

[[ ! $# -eq 1 ]] && echo -e "Usage:\n\t$0 <hex_jobId>" && exit 1;
CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

$CURL -v -H "Content-Type: application/json" \
	http://localhost:8080/e2data/nsga2/"$1"/plans
