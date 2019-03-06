#!/usr/bin/env bash

mkdir -p bin
javac -d bin  -cp xjsnark_backend.jar ./src/xjsnark/blake/Blake2s.java
