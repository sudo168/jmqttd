package net.ewant.jmqttd.config;

import java.io.IOException;
import java.io.InputStream;

public interface ConfigLoader {
    InputStream load(String path) throws IOException;
}
