package org.richardcarter;

import com.codahale.metrics.Meter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GenerateChunkFile implements Runnable {
    private final IpAddress startInclusive;
    private final IpAddress endExclusive;
    private final String filename;
    private final Meter meter;

    @Getter
    private IpAddress currentIp;

    @Override
    public void run() {
        if (startInclusive.compareTo(endExclusive) >= 0) {
            throw new IllegalArgumentException("start >= end");
        }
        try (StorageWriter writer = new StorageWriter(filename)) {
            currentIp = startInclusive;
            while (!currentIp.equals(endExclusive)) {
                byte[] hash = Hasher.calculateHash(currentIp);
                writer.write(currentIp, hash);
                meter.mark();
                currentIp = currentIp.increment();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
