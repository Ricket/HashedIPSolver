Hashed IP Solver
================
Lookup table for IPv4 address and SHA-1 hash with optional salt. Java 17.

Disk Space
----------
The lookup table will take just over 100 GB of disk space. (4 bytes for IP + 20 bytes for hash) * 4 billion IP addresses.

If you wanted to save a few GB you could skip private IP spaces. I wanted to use the entire range of IPv4 to avoid
any possibility of a false negative.

Usage
-----
1. Fill in `Hasher.SALT`
2. Check out the `Main.main` method for the various methods to call. You should uncomment & run the generate method,
   then uncomment & run the sort method, and then run the hash lookup method each time after that.

Development
-----------
I iterated on the approach, so I left a lot of the old code here since this was just for fun. I also did not make this
into a usable CLI tool, it's meant to just be run in the IDE by uncommenting the desired operation.

I discovered that a single 100GB file was too unwieldy; so I split it into chunks and then I was able to more
efficiently generate the chunks in parallel, and also able to sort them in-memory which was much faster than my
first on-disk quicksort approach.

Sorting is very important. Iterating the whole table to find a hash took upwards of 5 minutes; but once it's sorted by
hash, then binary searching through the table (chunked) takes about half a second.