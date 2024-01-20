package org.richardcarter;

import com.codahale.metrics.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static final String FILENAME = "ipsAndHashes.bin";
    public static final String CHUNKED_FILENAME_PATTERN = "hashLookup%d.bin";

    public static void main(String[] args) throws Exception {
        // Recommended procedure:
        // 1. writeChunkedFiles(args); -- writes the chunked files; one file per CPU logical processor, in parallel.
        // 2. sortChunkedFiles(args); -- sorts each chunked file, one at a time (to not overwhelm RAM).
        // 3. binarySearchChunkedFiles(args); -- binary search for a desired hash in the chunked files. (update the `desiredHash` variable)

        // Older attempts:
        // parallel(args); -- generate one large lookup table file, but do the hashing in parallel. slower than series.
        // series(args); -- generate one large lookup table file, using just one thread. this seemed optimal; I/O is the bottleneck.
        // findIp(args); -- search the large lookup table file for a hash. linear search.
        // sortInPlace(args); -- sort the large lookup table file in-place using quicksort. extremely slow; it ran for 8 hours and didn't finish.
    }

    /**
     * Binary search the lookup table files.
     * Just single-threaded searches them in order.
     */
    public static void binarySearchChunkedFiles(String[] args) throws Exception {
        byte[] desiredHash = new byte[] {
                // 20 bytes of hash go here
        };

        int numChunks = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numChunks; i++) {
            String filename = String.format(CHUNKED_FILENAME_PATTERN, i);
            System.out.println(filename);
            IpAddress foundIp = new BinarySearcher(filename).search(desiredHash);
            if (foundIp != null) {
                System.out.println(foundIp);
                break;
            }
        }
        System.out.println("done");
    }

    /**
     * Sort the lookup table files.
     * Since each file is large, sorts them one at a time in a single thread.
     * The file is loaded into memory, sorted, then overwritten back to disk.
     */
    public static void sortChunkedFiles(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();

        Meter readMeter = metrics.meter("readBytes");
        Timer sortTimer = metrics.timer("sortTime");
        Meter writeMeter = metrics.meter("writeBytes");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(5, TimeUnit.SECONDS);

        // You can't do all of these in parallel (unless you have > 100GB RAM) so we do them linearly.
        // If you wanted to do this sort faster, you could do a few in parallel at a time.

        int numChunks = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numChunks; i++) {
            String filename = String.format(CHUNKED_FILENAME_PATTERN, i);
            StorageSortInMemory sorter = new StorageSortInMemory(filename, readMeter, sortTimer, writeMeter);
            sorter.sort();
        }

        System.out.println("done");
    }

    /**
     * Write the lookup table as a set of files.
     * First it divides the IPv4 space into N regions, where N = number of logical processors.
     * And then, in parallel, generates & saves those regions to numbered output files.
     */
    public static void writeChunkedFiles(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();

        int numChunks = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numChunks);

        long max = (new IpAddress(255, 255, 255, 255).toLong() & 0xFFFFFFFFL) + 1L;
        long increment = max / numChunks;
        IpAddress[] ips = new IpAddress[numChunks + 1];
        ips[numChunks] = new IpAddress(255, 255, 255, 255);

        for (int i = 0; i < numChunks; i++) {
            ips[i] = IpAddress.fromLong(increment * i);
        }

        for (int i = 0; i < numChunks; i++) {
            IpAddress start = ips[i];
            IpAddress end = ips[i + 1];
            Meter meter = metrics.meter("generator-" + i);
            GenerateChunkFile generator = new GenerateChunkFile(start, end,
                    String.format(CHUNKED_FILENAME_PATTERN, i), meter);
            executorService.submit(generator);
        }

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(5, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.MINUTES);

        System.out.println("done");
    }

    /**
     * Sort the single large lookup file in-place on disk.
     * I ran it for 8 hours and it didn't complete.
     */
    @Deprecated
    public static void sortInPlace(String[] args) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(FILENAME, "rw")) {
            new StorageSortInPlace(file).quickSort();
        }
    }

    /**
     * Linear-search the large lookup file for a hash.
     */
    @Deprecated
    public static void findIp(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();

        AtomicReference<byte[]> lastIp = new AtomicReference<>();
        Gauge<String> currentIpGauge = metrics.gauge("currentIp", () -> new Gauge<String>() {
            @Override
            public String getValue() {
                byte[] ip = lastIp.get();
                if (ip == null) {
                    return null;
                }
                return IpAddress.fromByteArray(ip).toString();
            }
        });

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(5, TimeUnit.SECONDS);

        byte[] desiredHash = new byte[] {
            // 20 bytes of hash goes here
        };
        if (desiredHash.length != 20) {
            throw new IllegalArgumentException();
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(FILENAME))) {
            while (true) {
                byte[] ip = bis.readNBytes(4);
                if (ip.length == 0) break;
                lastIp.set(ip);
                byte[] hash = bis.readNBytes(20);
                if (Arrays.equals(hash, desiredHash)) {
                    IpAddress ipAddress = IpAddress.fromByteArray(ip);
                    System.out.println(ipAddress);
                    break;
                }
            }
        }

        System.out.println("done");
    }

    /**
     * Generate the large lookup table file, in a single thread.
     */
    @Deprecated
    public static void series(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();

        Meter hashRate = metrics.meter("hashRate");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(5, TimeUnit.SECONDS);

        IpAddress ip = IpAddress.MIN_VALUE;
        SettableGauge<IpAddress> currentIp = new DefaultSettableGauge<>(ip);
        metrics.register("currentIp", currentIp);
        try (StorageWriter writer = new StorageWriter("ipsAndHashes.bin")) {
            while (!ip.equals(IpAddress.MAX_VALUE)) {
                currentIp.setValue(ip);
                byte[] hash = Hasher.calculateHash(ip);
                hashRate.mark();
                writer.write(ip, hash);
                ip = ip.increment();
            }
        }
    }

    /**
     * Generate the single large lookup table using workers in parallel.
     * It seems the CPU processing of SHA-1 is not the bottleneck, so this ended up being slower than doing it
     * in a single thread, likely due to the contention of the workers depositing the results into the shared results
     * queue.
     * It might be able to be improved, maybe workers could add in batches for example.
     */
    @Deprecated
    public static void parallel(String[] args) throws Exception {
        MetricRegistry metrics = new MetricRegistry();

        Meter hashRate = metrics.meter("hashRate");

        BlockingQueue<IpAndHash> hashResults = new ArrayBlockingQueue<>(4000000, false);
        metrics.register("resultsQueueDepth", (Gauge<Integer>) hashResults::size);

        StorageWriter writer = new StorageWriter("ipsAndHashes.bin");
        Histogram batchSize = metrics.histogram("writerBatchSize");
        StorageWriterConsumer writerConsumer = new StorageWriterConsumer(hashResults, writer, batchSize);
        Thread writerConsumerThread = new Thread(writerConsumer);

        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(5, TimeUnit.SECONDS);

        writerConsumerThread.start();

        Semaphore semaphore = new Semaphore(200);

        IpAddress ip = IpAddress.MIN_VALUE;
        SettableGauge<IpAddress> currentIp = new DefaultSettableGauge<>(ip);
        metrics.register("currentIp", currentIp);
        IpAddress max = IpAddress.MAX_VALUE;
        while (!ip.equals(max)) {
            semaphore.acquire();
            executorService.submit(new GenerateHashTask(ip, hashResults, hashRate, semaphore));
            ip = ip.increment();
            currentIp.setValue(ip);
        }

        executorService.shutdown();

        Thread.sleep(1);

        writerConsumer.shutdown();
        writerConsumerThread.join();

        reporter.stop();
    }
}