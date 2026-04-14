package com.chessmate.infra_redis.sync;

public enum SyncQueueKey {

    LICHESS("queue:sync:lichess"),
    CHESSCOM("queue:sync:chesscom");

    private final String key;

    SyncQueueKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}