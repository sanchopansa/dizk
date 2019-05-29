#!/usr/bin/env bash

function usage {
    echo "usage: ./create_circuit output_directory num_bytes_input"
    exit 1
}

[[ $# -ne 2 ]] && usage

mkdir -p $1
java -cp bin:xjsnark_backend.jar profiler.blake.circuits.xjsnark.blake.Blake2s $1 $2
