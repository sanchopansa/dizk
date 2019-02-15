package profiler;

import configuration.Configuration;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import profiler.profiling.Blake2sProfiling;
import profiler.utils.SparkUtils;

public class Blake2sProfiler {

    private static void serialApp(final String app, final String filePath) {
        System.out.format("\n[Profiler] - Start Serial %s from %s\n",
                SparkUtils.appName(app), filePath);

        Blake2sProfiling.serialZKSnarkProfiler(new Configuration(), filePath);

        System.out.format("\n[Profiler] - End Serial %s from %s\n",
                SparkUtils.appName(app), filePath);
    }

    private static void distributedApp(final String app, final Configuration config) {
        System.out.format(
                "\n[Profiler] - Start Distributed %s - %d executors - %d partitions\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions());

        Blake2sProfiling.distributedZKSnarkProfiler(config, "");

        System.out.format(
                "\n[Profiler] - End Distributed %s - %d executors - %d partitions\n\n",
                SparkUtils.appName(app), config.numExecutors(), config.numPartitions());
    }

    public static void main(String[] args) {
        final int numExecutors = 4;
        final int numCores = 4;
        final int numMemory = 4;
        final int numPartitions = 4;

        final SparkSession spark = SparkSession
                .builder()
                .master("local[4]")
                .appName(SparkUtils.appName("blake2s"))
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

        distributedApp("blake2s", config);
//        serialApp("blake2s", "");
//        if (args.length > 0) {
//            String appType = args[0].toLowerCase();
//            String app = "blake2s";
//
//            if (appType.equals("serial")) {
//                final String filePath = args[0];
//                final String filePath = "";
//                serialApp(app, filePath);
//            } else if (appType.equals("distributed")) {
//                final String filePath = args[2];
//                final int numExecutors = Integer.parseInt(args[3]);
//                final int numCores = Integer.parseInt(args[4]);
//                final int numMemory = Integer.parseInt(args[5].substring(0, args[6].length() - 1));
//                final int numPartitions = Integer.parseInt(args[6]);
//
//
//                final SparkSession spark = SparkSession.builder().appName(SparkUtils.appName(app)).getOrCreate();
//                spark.sparkContext().conf().set("spark.files.overwrite", "true");
//                spark.sparkContext().conf().set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
//                spark.sparkContext().conf().registerKryoClasses(SparkUtils.zksparkClasses());
//
//                JavaSparkContext sc;
//                sc = new JavaSparkContext(spark.sparkContext());
//
//                final Configuration config = new Configuration(
//                        numExecutors,
//                        numCores,
//                        numMemory,
//                        numPartitions,
//                        sc,
//                        StorageLevel.MEMORY_AND_DISK_SER());
//
//                distributedApp(app, config);
//            } else {
//                System.out.format("App type %s not found", appType);
//            }
//        } else {
//            System.out.println(
//                    "Args: {appType} {app} {filePath} {numExecutors(opt)} {numCores(opt)} {numMemory(opt)} {numPartitions(opt)}");
//        }
    }

}
