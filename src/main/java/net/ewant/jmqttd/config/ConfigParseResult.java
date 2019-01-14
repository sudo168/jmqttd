package net.ewant.jmqttd.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigParseResult<T> {

    List<Throwable> causes;

    T result;

    public ConfigParseResult(){
        this.causes = new ArrayList<>();
    }

    public void addCause(Throwable cause){
        this.causes.add(cause);
    }

    public void addAllCause(List<Throwable> causes){
        this.causes.addAll(causes);
    }

    public List<Throwable> getCauses() {
        return causes;
    }

    public void addResult(T result){
        this.result = result;
    }

    public T getResult() {
        return result;
    }
}
