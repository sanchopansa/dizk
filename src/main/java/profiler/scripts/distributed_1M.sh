#!/usr/bin/env bash

# Copy project to worker nodes
/root/spark-ec2/copy-dir /home/ec2-user/

export JAVA_HOME="/usr/lib/jvm/java-1.8.0"

export APP_TYPE='distributed'
export APP='input-feed'
export MEMORY=13G
export SIZE=20
export FILE_PATH="/data/hash_1M/hash_transform"
export CORES=2
export NUM_EXECUTORS=20
export NUM_PARTITIONS=10

/root/spark/bin/spark-submit \
  --conf spark.driver.memory=${MEMORY} \
  --conf spark.driver.maxResultSize=4G \
  --conf spark.executor.cores=${CORES} \
  --total-executor-cores ${NUM_EXECUTORS} \
  --conf spark.executor.memory=${MEMORY} \
  --conf spark.memory.fraction=0.95 \
  --conf spark.memory.storageFraction=0.3 \
  --conf spark.kryoserializer.buffer.max=1g \
  --conf spark.rdd.compress=true \
  --conf spark.rpc.message.maxSize=1024 \
  --conf spark.executor.heartbeatInterval=30s \
  --conf spark.network.timeout=300s\
  --conf spark.speculation=true \
  --conf spark.speculation.interval=5000ms \
  --conf spark.speculation.multiplier=1 \
  --conf spark.local.dir=/mnt/spark \
  --conf spark.logConf=true \
  --conf spark.eventLog.dir=/tmp/spark-events \
  --conf spark.eventLog.enabled=false \
  --class "profiler.InputProfiler" \
  /home/ec2-user/dizk-1.0.jar ${APP_TYPE} ${APP} ${FILE_PATH} ${NUM_EXECUTORS} ${CORES} ${MEMORY} ${NUM_PARTITIONS}