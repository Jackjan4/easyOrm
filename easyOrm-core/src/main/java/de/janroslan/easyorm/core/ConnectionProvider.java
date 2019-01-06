package de.janroslan.easyorm.core;

import java.sql.Connection;

public interface ConnectionProvider {

    Connection getConnection(ConnectionHandle handle);

    void usageEnded(ConnectionHandle handle);
}
