package org.richardcarter;

import com.codahale.metrics.Meter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

@RequiredArgsConstructor
public class GenerateHashTask implements Runnable {
    private final IpAddress ip;
    private final BlockingQueue<IpAndHash> resultQueue;
    private final Meter hashRate;
    private final Semaphore semaphore;

    @Override
    public void run() {
        try {
            byte[] hash = Hasher.calculateHash(ip);
            hashRate.mark();
            resultQueue.put(new IpAndHash(ip, hash));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();
        }
    }
}
