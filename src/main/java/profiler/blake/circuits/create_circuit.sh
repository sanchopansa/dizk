#!/usr/bin/env bash

function usage {
    echo "usage: ./create_circuit output_directory num_bytes_input"
    exit 1
}

[[ $# -ne 2 ]] && usage

java -cp bin:xjsnark_backend.jar xjsnark.blake.Blake2s $1 $2
