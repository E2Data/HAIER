#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP OPTIONS request to emulate a CORS preflight request issued
# before attempting to configure the NSGA-II algorithm of HAIER's optimizer,
# assuming HAIER is at localhost:8080.

CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

$CURL -v -X OPTIONS \
	-H "Origin: http://random.example.com" \
	-H "Access-Control-Request-Method: PUT" \
	http://localhost:8080/e2data/nsga2/params

