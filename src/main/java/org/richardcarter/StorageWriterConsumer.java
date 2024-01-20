package org.richardcarter;

import com.codahale.metrics.Histogram;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
public class StorageWriterConsumer implements Runnable {
    private final BlockingQueue<IpAndHash> hashResults;
    private final StorageWriter writer;
    private final Histogram batchSize;

    private volatile boolean shutdown = false;

    @Override
    public void run() {
        while (!shutdown) {
            ArrayList<IpAndHash> batch = new ArrayList<>();
            hashResults.drainTo(batch);
            batchSize.update(batch.size());
            for (IpAndHash result : batch) {
                writer.write(result.getIp(), result.getHash());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        shutdown = true;
    }
}
