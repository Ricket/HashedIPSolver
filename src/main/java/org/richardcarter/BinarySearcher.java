package org.richardcarter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.RandomAccessFile;
import java.util.Arrays;

@RequiredArgsConstructor
public class BinarySearcher {
    private final String filename;

    @SneakyThrows
    public IpAddress search(byte[] targetHash) {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            long lengthBytes = file.length();
            if (lengthBytes % 24 != 0) {
                throw new IllegalArgumentException(filename + " length is not a multiple of 24");
            }
            long countEntries = lengthBytes / 24L;
            System.out.println(countEntries);

            long left = 0;
            long right = countEntries;
            while (true) {
                long idx = (left + right) / 2;
                file.seek(idx * 24L);
                byte[] ip = new byte[4];
                file.read(ip);
                byte[] hash = new byte[20];
                file.read(hash);

                int compare = Arrays.compare(hash, targetHash);
                if (compare == 0) {
                    return IpAddress.fromByteArray(ip);
                }

                if (compare > 0) {
                    if (idx == right) {
                        break;
                    }
                    right = idx;
                } else {
                    if (idx == left) {
                        break;
                    }
                    left = idx;
                }
            }
        }
        return null;
    }
}
