# Profiling Tips, Tricks and Troubleshooting

Once logged into the AWS cluster,

1. Setup environment

```
cd /scripts
./setup_environment.sh
```



2. Mount the constraint system data
First ensure that the EBS volume is attached (from the EC2 Dashboard)

```
mkdir /data && mount /dev/xvdf /data
```

3. Configure and run profiling script (see ./profile.sh as template)

The communications are sometimes cut off and currently running jobs are halted. It is best to use nohup (no hangup) and run these processes in the background.

```
nohup ./profile.sh &
```
Logs from this process are written to *./nohup.out*.


## Little hack for EBS volumes

All the data needs to be available for each worker node,
but the EBS volume can only be mounted onto a single machine at any given time.
Additionally, as these EC2 instances only have 8Gb of storage,
we copy all the constraint system data to the mounted hadoop volume.

```bash
cp -r /data/zksnarks/ /mnt/hadoop/
/root/spark-ec2/copy-dir /mnt/hadoop

# unzip on master
cd /mnt/hadoop
gunzip -r zksnarks/

# unzip on all slaves
SLAVES=`cat /root/spark-ec2/slaves`

for slave in $SLAVES; do
    echo "unzipping compressed data files ${slave}"
    ssh $slave 'cd /mnt/hadoop; gunzip -r zksnarks/'
done
```

