package de.janroslan.easyorm.core;

public class ColumnDefinition {

    private String name;
    private boolean notNull;
    private DataType dataType;
    private boolean primaryKey;
    private boolean autoIncrement;
    private boolean foreignKey;
    private ForeignKeyRef foreignRef;


    /**
     *
     * @param name
     * @param notnull
     * @param dataType
     * @param primaryKey
     * @param foreignRef
     */
    public ColumnDefinition(String name, boolean notnull, DataType dataType, boolean primaryKey, ForeignKeyRef foreignRef) {
        this.name = name;
        this.notNull = notnull;
        this.dataType = dataType;
        this.primaryKey = primaryKey;
        this.foreignKey = true;
        this.foreignRef = foreignRef;
        this.autoIncrement = false;
    }


    /**
     *
     * @param name
     * @param notnull
     * @param dataType
     * @param primaryKey
     */
    public ColumnDefinition(String name, boolean notnull, DataType dataType, boolean primaryKey, boolean autoIncrement) {
        this.name = name;
        this.notNull = notnull;
        this.dataType = dataType;
        this.primaryKey = primaryKey;
        this.foreignKey = false;
        this.foreignRef = null;
        this.autoIncrement = autoIncrement;
    }


    /**
     *
     * @param name
     * @param notnull
     * @param dataType
     */
    public ColumnDefinition(String name, boolean notnull, DataType dataType) {
        this.name = name;
        this.notNull = notnull;
        this.dataType = dataType;
        this.primaryKey = false;
        this.foreignKey = false;
        this.foreignRef = null;
    }

    @Override
    public String toString() {
        String str = getName() + " " + getDataType().name();

        if (isNotNull() && !isPrimaryKey()) {
            str += " NOT NULL";
        }
        return str;
    }

    public String getName() {
        return name;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public DataType getDataType() {
        return dataType;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isForeignKey() {
        return foreignKey;
    }

    public ForeignKeyRef getForeignRef() {
        return foreignRef;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public enum DataType {
        TEXT,
        REAL,
        INTEGER
    }

    public static class ForeignKeyRef {

        private String tableName;
        private String columnName;


        public ForeignKeyRef(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getColumnName() {
            return columnName;
        }
    }
}
