#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP POST request to ML inference mock service, similar to the one
# HAIER produces within the TornadoModel. The mock service is assumed to be
# listening on localhost:58888.

data='{"Objective":"execTime","Device":{"maximumAllocation":0,"minimumAllocation":0,"name":"uber-gpu","units":null,"value":0,"host":"gold2"},"TornadoFeatures":{"bean":{"Device ID":null,"Device":null,"Global Memory Loads":0,"Global Memory Stores":0,"Local Memory Loads":0,"Local Memory Stores":0,"Constant Memory Loads":0,"Constant Memory Stores":0,"Private Memory Loads":0,"Private Memory Stores":0,"Total Loops":0,"Parallel Loops":0,"If Statements":0,"Switch Statements":0,"Switch Cases":0,"Cast Operations":0,"Vector Operations":0,"Total Integer Operations":0,"Total Float Operations":0,"Single Precision Float Operations":0,"Double Precision Float Operations":0,"Binary Operations":0,"Boolean Operations":0,"Float Math Functions":0,"Integer Math Functions":0,"Integer Comparison":0,"Float Comparison":0}}}'

CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;

curl -v -X POST -d "$data" http://localhost:58888/
