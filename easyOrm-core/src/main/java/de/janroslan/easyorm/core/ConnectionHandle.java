package de.janroslan.easyorm.core;

import java.util.Random;

public class ConnectionHandle {

    private String tableName;
    private int handleId;


    public ConnectionHandle(String tableName) {
        this.tableName = tableName;
        handleId = new Random().nextInt();
    }

    public String getTableName() {
        return tableName;
    }

    public int getHandleId() {
        return handleId;
    }
}
