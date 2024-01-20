package org.richardcarter;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;

public class StorageSortInPlace {
    private final RandomAccessFile file;

    private final long entryCount;

    public StorageSortInPlace(RandomAccessFile file) throws IOException {
        this.file = file;
        long length = file.length();
        if (length % 24 != 0) {
            throw new IllegalArgumentException("length not evenly divisible by 24");
        }
        entryCount = length / 24;
    }

    public void quickSort() {
        quickSort(0, entryCount - 1);
    }

    @SneakyThrows
    private IpAndHash get(long idx) {
        if (idx < 0 || idx >= entryCount) {
            throw new IllegalArgumentException("idx out of bounds: " + idx + " / entryCount: " + entryCount);
        }
        file.seek(idx * 24);
        byte[] ip = new byte[4];
        file.read(ip);
        byte[] hash = new byte[20];
        file.read(hash);

        IpAddress ipAddress = IpAddress.fromByteArray(ip);
        return new IpAndHash(ipAddress, hash);
    }

    @SneakyThrows
    private void set(long idx, IpAndHash newValue) {
        if (idx < 0 || idx >= entryCount) {
            throw new IllegalArgumentException("idx out of bounds: " + idx + " / entryCount: " + entryCount);
        }
        file.seek(idx * 24);
        file.write(newValue.getIp().toBytes());
        file.write(newValue.getHash());
    }

    private void quickSort(long begin, long end) {
        if (begin < end) {
            long partitionIndex = partition(begin, end);
            quickSort(begin, partitionIndex - 1);
            quickSort(partitionIndex + 1, end);
        }
    }

    private long partition(long begin, long end) {
        IpAndHash pivot = get(end);
        long i = (begin - 1);
        for (long j = begin; j < end; j++) {
            IpAndHash ipJ = get(j);
            if (CompareHashes.INSTANCE.compare(ipJ, pivot) <= 0) {
                i++;

                IpAndHash swapTemp = get(i);
                set(i, ipJ);
                set(j, swapTemp);
            }
        }

        IpAndHash swapTemp = get(i + 1);
        set(i + 1, pivot);
        set(end, swapTemp);

        return i + 1;
    }

    private static class CompareHashes implements Comparator<IpAndHash> {
        public static CompareHashes INSTANCE = new CompareHashes();

        @Override
        public int compare(IpAndHash o1, IpAndHash o2) {
            return Arrays.compare(o1.getHash(), o2.getHash());
        }
    }

}
