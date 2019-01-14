package net.ewant.jmqttd.config;

import java.io.IOException;
import java.io.InputStream;

public class ClassPathConfigLoader implements ConfigLoader{

    public InputStream load(String path) throws IOException {
        if(path == null){
            return null;
        }
    	return this.getClass().getClassLoader().getResourceAsStream(path);
    }
}
