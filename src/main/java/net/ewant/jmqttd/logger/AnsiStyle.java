package net.ewant.jmqttd.logger;

/**
 * Created by admin on 2018/11/20.
 */
public enum AnsiStyle implements AnsiElement {

    NORMAL("0"),

    BOLD("1"),

    FAINT("2"),

    ITALIC("3"),

    UNDERLINE("4");

    private final String code;

    AnsiStyle(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }

}
