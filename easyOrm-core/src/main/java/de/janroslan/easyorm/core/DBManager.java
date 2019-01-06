package de.janroslan.easyorm.core;


public abstract class DBManager {


    public DBManager() {

    }


    public abstract ConnectionProvider getConnectionProvider();

}
