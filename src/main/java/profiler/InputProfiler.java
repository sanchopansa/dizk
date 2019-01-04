package profiler;

import configuration.Configuration;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import profiler.profiling.*;
import profiler.utils.SparkUtils;

public class InputProfiler {

    private static void serialApp(final String app, final String filePath) {
        System.out.format("\n[Profiler] - Start Serial %s from %s\n",
                SparkUtils.appName(app), filePath);

        if (app.equals("input-feed")) {
            InputFeedProfiling.serialZKSnarkProfiler(new Configuration(), filePath);
        } else {
            System.out.println("[Error] Serial app doesn't exist");
        }

        System.out.format("\n[Profiler] - End Serial %s from %s\n",
                SparkUtils.appName(app), filePath);
    }

    private static void distributedApp(final String app, final Configuration config) {
        System.out.format(
                "\n[Profiler] - Start Distributed %s - %d executors - %d partitions - from %s\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions(), config.filePath());

        if (app.equals("input-feed")) {
            InputFeedProfiling.distributedZKSnarkProfiler(config, config.filePath());
        } else {
            System.out.println("[Error] Distributed app doesn't exist");
        }

        System.out.format(
                "\n[Profiler] - End Distributed %s - %d executors - %d partitions - from %s\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions(), config.filePath());
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String appType = args[0].toLowerCase();
            String app = args[1].toLowerCase();

            if (appType.equals("serial")) {
                final String filePath = args[2];
                serialApp(app, filePath);

            } else if (appType.equals("distributed")) {
                final String filePath = args[2];
                final int numExecutors = Integer.parseInt(args[3]);
                final int numCores = Integer.parseInt(args[4]);
                final int numMemory = Integer.parseInt(args[5].substring(0, args[5].length() - 1));
                final int numPartitions = Integer.parseInt(args[6]);


                final SparkSession spark = SparkSession.builder().appName(SparkUtils.appName(app)).getOrCreate();
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
                        StorageLevel.MEMORY_AND_DISK_SER(),
                        filePath);

                distributedApp(app, config);
            } else {
                System.out.format("App type %s not found", appType);
            }
        } else {
            System.out.println(
                    "Args: {appType} {app} {filePath} {numExecutors(opt)} {numCores(opt)} {numMemory(opt)} {numPartitions(opt)}");
        }
    }

}
