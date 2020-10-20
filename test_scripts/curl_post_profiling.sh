#!/bin/bash
#
# author: ckatsak
#
# Issue a HTTP POST request to submit profiling information to HAIER (in the
# format (JSON) specified by Tornado), assuming HAIER is at localhost:8080.

## Usage/prereqs
[[ $# -gt 1 ]] && echo -e "Usage:\n\t$0 [<prof_info.json>]" && exit 1;
CURL=$(which curl)
[[ -z $CURL ]] && echo "Executable 'curl' cannot be found in PATH" && exit 2;


## hard-coded testcases definition
read -r -d '' s1 <<EOS
{
    "s0": {
        "TOTAL_BYTE_CODE_GENERATION": "6840603",
        "TOTAL_KERNEL_TIME": "0",
        "TOTAL_GRAAL_COMPILE_TIME": "31683624",
        "TOTAL_TASK_SCHEDULE_TIME": "115224482",
        "COPY_OUT_TIME": "0",
        "TOTAL_DRIVER_COMPILE_TIME": "64397610",
        "s0.t0": {
            "DEVICE_ID": "0:0",
            "DEVICE": "GeForce GTX 1050",
            "TASK_COPY_OUT_SIZE_BYTES": "152",
            "TASK_KERNEL_TIME": "0",
            "TASK_COMPILE_GRAAL_TIME": "31683624",
            "TASK_COMPILE_DRIVER_TIME": "64397610"
        }
    }
}
EOS
read -r -d '' s2 <<EOS
{
    "s3": {
        "TOTAL_BYTE_CODE_GENERATION": "6840603",
        "TOTAL_KERNEL_TIME": "0",
        "TOTAL_GRAAL_COMPILE_TIME": "31683624",
        "TOTAL_TASK_SCHEDULE_TIME": "115224482",
        "COPY_OUT_TIME": "0",
        "TOTAL_DRIVER_COMPILE_TIME": "64397610",
        "s2.t1": {
            "DEVICE_ID": "1:2",
            "DEVICE": "GeForce GTX 1060",
            "TASK_COPY_OUT_SIZE_BYTES": "152",
            "TASK_KERNEL_TIME": "0",
            "TASK_COMPILE_GRAAL_TIME": "31683624",
            "TASK_COMPILE_DRIVER_TIME": "64397610"
        }
    }
}
EOS
set -euo pipefail
echo $s1 | jq '.' >/dev/null
echo $s2 | jq '.' >/dev/null
set +euo pipefail


## actual test
if [[ $# -eq 0 ]]; then
	$CURL -v -H "Content-Type: application/json" -X POST -d "$s1" \
		http://localhost:8080/e2data/profiling
	$CURL -v -H "Content-Type: application/json" -X POST -d "$s2" \
		http://localhost:8080/e2data/profiling
else
	$CURL -v -H "Content-Type: application/json" -X POST -d "@$1" \
		http://localhost:8080/e2data/profiling
fi
