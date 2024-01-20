package org.richardcarter;

import lombok.Value;

@Value
public class IpAndHash {
    IpAddress ip;
    byte[] hash;
}
