package profiler;

import configuration.Configuration;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import profiler.profiling.Blake2sProfiling;
import profiler.utils.SparkUtils;

public class Blake2sProfiler {

    private static void serialApp(final String app, final String circuitFile, final String inputFile) {
        System.out.format("\n[Profiler] - Start Serial %s from %s\n",
                SparkUtils.appName(app), circuitFile);

        Blake2sProfiling.serialZKSnarkProfiler(new Configuration(), circuitFile, inputFile);

        System.out.format("\n[Profiler] - End Serial %s from %s\n",
                SparkUtils.appName(app), circuitFile);
    }

    private static void distributedApp(final String app, final Configuration config, final String circuitFile, final String inputFile) {
        System.out.format(
                "\n[Profiler] - Start Distributed %s - %d executors - %d partitions\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions());

        Blake2sProfiling.distributedZKSnarkProfiler(config, circuitFile, inputFile);

        System.out.format(
                "\n[Profiler] - End Distributed %s - %d executors - %d partitions\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions());
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String appType = args[0].toLowerCase();
            String app = "blake2s";

            if (appType.equals("serial")) {
                final String circuitFile = args[1];
                final String inputFile = args[2];
                serialApp(app, circuitFile, inputFile);
            } else if (appType.equals("distributed")) {
                final String circuitFile = args[1];
                final String inputFile = args[2];
                final int numExecutors = Integer.parseInt(args[3]);
                final int numCores = Integer.parseInt(args[4]);
                final int numMemory = Integer.parseInt(args[5].substring(0, args[5].length() - 1));
                final int numPartitions = Integer.parseInt(args[6]);


                final SparkSession spark = SparkSession
                        .builder()
                        .appName(SparkUtils.appName(app))
                        .getOrCreate();
                spark.sparkContext().conf().set("spark.files.overwrite", "true");
                spark.sparkContext().conf().set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
                spark.sparkContext().conf().registerKryoClasses(SparkUtils.zksparkClasses());

                JavaSparkContext sc;
                sc = new JavaSparkContext(spark.sparkContext());

                final Configuration config = new Configuration(
                        numExecutors,
                        numCores,
                        numMemory,
                        numPartitions,
                        sc,
                        StorageLevel.MEMORY_AND_DISK_SER());

                distributedApp(app, config, circuitFile, inputFile);
            } else {
                System.out.format("App type %s not found", appType);
            }
        } else {
            System.out.println(
                    "Args: {appType} {app} {filePath} {numExecutors(opt)} {numCores(opt)} {numMemory(opt)} {numPartitions(opt)}");
        }
    }

}
