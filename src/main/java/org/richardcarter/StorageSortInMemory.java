package org.richardcarter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@RequiredArgsConstructor
public class StorageSortInMemory {

    private final String filename;
    private final Meter readMeter;
    private final Timer sortTimer;
    private final Meter writeMeter;

    @SneakyThrows
    public void sort() {
        System.out.println(filename);
        long size = Files.size(Path.of(filename));
        if ((size % 24) != 0) {
            throw new IllegalArgumentException("File not divisible by 24: " + filename);
        }
        long entryCount = size / 24;
        if (entryCount <= 0 || entryCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unexpected entryCount: " + entryCount);
        }
        IpAndHash[] entries = new IpAndHash[(int) entryCount];
        int i = 0;
        System.out.println("Reading " + entryCount + " entries...");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename))) {
            while (true) {
                byte[] ip = in.readNBytes(4);
                if (ip.length != 4) {
                    break;
                }
                byte[] hash = in.readNBytes(20);
                readMeter.mark(24);
                entries[i] = new IpAndHash(IpAddress.fromByteArray(ip), hash);
                i++;
            }
        }
        if (i != entryCount) {
            throw new IllegalStateException(String.format(
                    "Expected %d entries but only read %d", entryCount, i));
        }

        System.out.println("Sorting...");

        // With entries as a List<>, Collections.sort was the slowest:
        //   entries.sort((i1, i2) -> Arrays.compare(i1.getHash(), i2.getHash()));
        // Then, costing 2x memory, entries.parallelStream().sorted() was faster, but myseriously OOM'd after several iterations:
        //   entries = entries.parallelStream()
        //           .sorted((i1, i2) -> Arrays.compare(i1.getHash(), i2.getHash()))
        //           .collect(Collectors.toList());
        // Finally, changing `entries` to an array and using Arrays.parallelSort was _much_ faster.
        // The sort takes 10 seconds on my computer.
        try (Timer.Context ignored = sortTimer.time()) {
            Arrays.parallelSort(entries, (i1, i2) -> Arrays.compare(i1.getHash(), i2.getHash()));
        }

        System.out.println("Writing the result...");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename))) {
            for (IpAndHash entry : entries) {
                out.write(entry.getIp().toBytes());
                out.write(entry.getHash());
                writeMeter.mark(24);
            }
        }

        System.out.println();
    }
}
