package com.github.tartaricacid.netmusic.config;

public enum MusicProviderType {
    NETEASE("163"),
    QQ("QQ");

    private final String shortLabel;

    MusicProviderType(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public MusicProviderType next() {
        return this == NETEASE ? QQ : NETEASE;
    }

    public static MusicProviderType fromString(String value) {
        if (value == null) {
            return NETEASE;
        }
        String normalized = value.trim().toUpperCase();
        for (MusicProviderType providerType : values()) {
            if (providerType.name().equals(normalized)) {
                return providerType;
            }
        }
        return NETEASE;
    }
}
