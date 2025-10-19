package me.jissee.jarsauth.manip.jar;

import java.util.Arrays;

public class JarEntryWrapper {
    private final String name;
    private final byte[] bytes;

    public JarEntryWrapper(String name, byte[] bytes) {
        this.name = name;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}
