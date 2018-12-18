#!/usr/bin/env bash

. init.sh

# Get master node URL
readonly MASTER=`${SPARK_EC2_PATH} -k ${AWS_KEYPAIR_NAME} -i ${AWS_KEYPAIR_PATH} --region=${AWS_REGION_ID} get-master ${AWS_CLUSTER_NAME} | grep amazonaws.com`

# Transfer project to master node
scp -i ${AWS_KEYPAIR_PATH} ${DIZK_REPO_PATH}/target/dizk-1.0.jar ec2-user@${MASTER}:/home/ec2-user/

# Copy test R1CS
#scp -i ${AWS_KEYPAIR_PATH} -r ${DIZK_REPO_PATH}/src/test/data/text/pephash ec2-user@${MASTER}:/home/ec2-user/test_data

# Login
${SPARK_EC2_PATH} \
  --key-pair=${AWS_KEYPAIR_NAME} \
  --identity-file=${AWS_KEYPAIR_PATH} \
  --region=${AWS_REGION_ID} \
  login ${AWS_CLUSTER_NAME}

