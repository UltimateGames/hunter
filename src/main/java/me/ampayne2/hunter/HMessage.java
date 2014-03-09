package me.ampayne2.hunter;

import me.ampayne2.ultimategames.api.message.Message;

public enum HMessage implements Message {
    CIVILIAN("Civilian", "&4You are one of the last. Stay alive."),
    HUNTER("Hunter", "&4Exterminate every last civilian on minecraftia..."),
    DEATH("Death", "&4%s was killed!"),
    WIN_HUNTERS("WinHunters", "&4The Hunters have exterminated the Civilians!"),
    WIN_CIVILIANS("WinCivilians", "&4The Hunters have failed to exterminate the Civilians!");

    private String message;
    private final String path;
    private final String defaultMessage;

    private HMessage(String path, String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDefault() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return message;
    }
}
