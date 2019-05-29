# Blake2s Profiling in DIZK

To run a Blake2s profiling job in DIZK:

* `cd circuits`
* `./compile.sh` 
* `./create_circuit.sh output_directory num_bytes_input`

The above should produce two files in `output_directory`:

* `Blake2s.arith`: containing the Blake2s circuit in Pinocchio format
* `Blake2s_Sample_Run1.in`: containing the inputs for the generated circuit

Note that the `num_bytes_input` must be a multiple of 512 (the message block size for Blake2s)

Now, you can start a Spark job. First, build the DIZK JAR, by running `mvn package` in the root directory of the repo
and then use the provided `run.sh` script to start the Spark Job, e.g.:

`./run_spark.sh src/dizk/target/dizk-1.0.jar Blake2s.arith Blake2s_Sample_Run1.in 6 6 12g 12 distributed`
