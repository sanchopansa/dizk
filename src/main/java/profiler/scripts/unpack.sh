#!/usr/bin/env bash

SLAVES=`cat /root/spark-ec2/slaves`

echo "Unpacking snark data on slaves..."
for slave in ${SLAVES}; do
    echo ${slave}
    ssh ${slave} 'cd /mnt/hadoop; gunzip -r zksnarks/ && echo "done!"'
done