package de.janroslan.easyorm.core;

import de.janroslan.easyorm.annotations.ForeignKey;
import de.janroslan.easyorm.utils.JsonUtils;
import de.janroslan.jputils.collections.Pair;
import de.janroslan.jputils.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class Table<T> {


    protected final ConnectionProvider connectionProvider;
    protected final String tableName;

    private Field[] tFields;
    private Class tClass;

    private final List<ColumnDefinition> columns;



    public Table(ConnectionProvider c, String tableName) {
        this.tableName = tableName;
        connectionProvider = c;

        tClass = (Class) ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
        tFields = tClass.getDeclaredFields();

        columns = createColumns();
        createTable(columns);


    }



    protected void seed() {

    }



    private List<ColumnDefinition> createColumns() {
        List<ColumnDefinition> result = new ArrayList<>();

        for (Field f : tFields) {

            // Skip fields that are are declared to disable mapping
            if (ReflectionUtils.hasAnnotation(f, "DoNotMap")) {
                continue;
            }

            boolean notNull = ReflectionUtils.hasAnnotation(f, "NotNull");
            boolean autoIncrement = ReflectionUtils.hasAnnotation(f, "AutoIncrement");
            boolean primaryKey = ReflectionUtils.hasAnnotation(f, "PrimaryKey");
            boolean foreignKey = ReflectionUtils.hasAnnotation(f, "ForeignKey");
            ColumnDefinition.DataType dataType = DataTypeConverter.JavaToSql(f.getType());
            String name = f.getName();

            if (foreignKey) {
                ForeignKey fk = f.getDeclaredAnnotation(ForeignKey.class);
                ColumnDefinition.ForeignKeyRef ref = new ColumnDefinition.ForeignKeyRef(fk.tableName(), fk.columnName());
                result.add(new ColumnDefinition(name, notNull, dataType, primaryKey, ref));
            } else {
                result.add(new ColumnDefinition(name, notNull, dataType, primaryKey, autoIncrement));
            }


        }

        return result;
    }



    private void createTable(List<ColumnDefinition> columns) {

        // no-op
        if (columns.size() == 0) {
            return;
        }

        List<ColumnDefinition> primaryKeys = new ArrayList<>();
        List<ColumnDefinition> foreignKeys = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        // Query header
        builder.append(String.format("CREATE TABLE IF NOT EXISTS %s (", tableName));


        // Append columns
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition cd = columns.get(i);
            builder.append(cd.toString());

            if (cd.isPrimaryKey()) {
                primaryKeys.add(cd);
            }

            if (cd.isForeignKey()) {
                foreignKeys.add(cd);
            }

            // if not last
            if (i < columns.size() - 1) {
                builder.append(", ");
            }
        }


        // Append primary keys
        if (primaryKeys.size() > 0) {
            builder.append(", PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                ColumnDefinition cd = primaryKeys.get(i);
                builder.append(cd.getName());

                // if not last
                if (i < primaryKeys.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }

        // Append foreign keys
        if (foreignKeys.size() > 0) {
            for (int i = 0; i < foreignKeys.size(); i++) {
                ColumnDefinition cd = foreignKeys.get(i);
                builder.append(String.format(", FOREIGN KEY (%s) REFERENCES %s(%s)", cd.getName(), cd.getForeignRef().getTableName(), cd.getForeignRef().getColumnName()));
            }
        }

        // Statement end
        builder.append(");");

        String sql = builder.toString();

        System.out.println(sql);

        var handle = new ConnectionHandle(getTableName());
        Connection c = connectionProvider.getConnection(handle);
        try {
            Statement smt = c.createStatement();
            smt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connectionProvider.usageEnded(handle);
        }
    }



    /**
     * Creates the model from the SQL result
     * Returns
     *
     * @param set
     * @return
     */
    protected T getModel(ResultSet set) {
        Object result = null;
        try {
            result = tClass.getDeclaredConstructor().newInstance();

            for (Field field : tFields) {
                String fName = field.getName();
                Object val = set.getObject(fName);
                Class c = field.getType();

                // LocalDateTime, boolean,... need special conversion from string-representation in sql
                val = DataTypeConverter.sqlToJavaConversion(c, val);


                // Convert from Json, if JsonConversion is declared on the field
                if (ReflectionUtils.hasAnnotation(field, "JsonConversion")) {
                    val = JsonUtils.toObj((String) val, field.getType());
                }
                ReflectionUtils.injectField(result, fName, val);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return (T) result;
    }



    public List<T> getAll() {
        return getFiltered();
    }



    /**
     * @param objects
     */
    public void add(T... objects) {
        var handle = new ConnectionHandle(getTableName());
        var connection = connectionProvider.getConnection(handle);
        add(connection, objects);
        connectionProvider.usageEnded(handle);
    }



    /**
     * @param objects
     */
    private void add(Connection c, T... objects) {
        List<String> values = new ArrayList<>();

        try {
            Statement smt = c.createStatement();

            StringBuilder builder;
            for (T t : objects) {
                builder = new StringBuilder();

                builder.append(String.format("INSERT INTO %s (", getTableName()));

                for (int i = 0; i < columns.size(); i++) {
                    Field f = tFields[i];
                    ColumnDefinition cd = columns.get(i);

                    // Skip AutoIncrement fields because they are automatic increment ;)
                    if (cd.isAutoIncrement()) {
                        continue;
                    }

                    try {
                        f.setAccessible(true);
                        Object obj = f.get(t);
                        if (obj == null) {
                            continue;
                        }
                        String s = obj.toString();
                        if (ReflectionUtils.hasAnnotation(f, "JsonConversion")) {
                            s = JsonUtils.toJson(obj);
                        }

                        if (cd.getDataType().equals(ColumnDefinition.DataType.TEXT)) {
                            s = "'" + s + "'";
                        }

                        builder.append(cd.getName());
                        values.add(s);


                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    if (i < columns.size() - 1) {
                        builder.append(", ");
                    }
                }
                builder.append(") VALUES (");

                for (int i = 0; i < values.size(); i++) {
                    String val = values.get(i);
                    builder.append(val);
                    if (i < values.size() - 1) {
                        builder.append(", ");
                    }
                }
                builder.append(")");

                smt.executeUpdate(builder.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


    }



    /**
     * @param obje
     */
    public int addReturnId(T obje) {
        int result = -1;

        var handle = new ConnectionHandle(getTableName());
        var connection = connectionProvider.getConnection(handle);

        try {
            // Disable auto commit
            connection.setAutoCommit(false);

            add(connection, obje);

            Statement smt = connection.createStatement();
            var generatedKeys = smt.executeQuery("SELECT last_insert_rowid()");
            if (generatedKeys.next()) {
                result = generatedKeys.getInt(1);
            }

            // Comit transaction
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connectionProvider.usageEnded(handle);
        }



        return result;
    }



    /**
     * @param filters
     * @return
     */
    public List<T> getFiltered(Pair<String, ?>... filters) {
        List<T> result = new ArrayList<>();
        String baseQuery = "SELECT * FROM " + tableName;


        // Only filter when filters are given
        if (filters.length != 0) {
            baseQuery += " WHERE ";

            for (Pair p : filters) {
                baseQuery += p.item1 + " LIKE '" + p.item2 + "'";
            }
        }
        result = executeSelect(baseQuery);

        return result;
    }



    /**
     * @param otherTableName
     * @param onJoin
     * @param conditions
     * @return
     */
    public List<T> getInnerJoin(String otherTableName, String onJoin, String... conditions) {
        List<T> result = new ArrayList<>();

        String sql = String.format("SELECT %s.* FROM %s INNER JOIN %s ON %s", getTableName(), getTableName(), otherTableName, onJoin);

        for (String s : conditions) {
            sql += " AND " + s;
        }

        sql += ";";

        result = executeSelect(sql);

        return result;
    }



    /**
     * @param sqlQuery
     * @return
     */
    private List<T> executeSelect(String sqlQuery) {
        List<T> result = new ArrayList<T>();

        // Statement execution
        var handle = new ConnectionHandle(getTableName());
        Connection c = connectionProvider.getConnection(handle);
        try {
            ResultSet sqlResult = c.createStatement().executeQuery(sqlQuery);

            while (sqlResult.next()) {
                result.add(getModel(sqlResult));
            }
        } catch (SQLException e) {
            System.out.println("error");
            e.printStackTrace(System.out);
        } finally {
            connectionProvider.usageEnded(handle);
        }

        return result;
    }



    public String getTableName() {
        return tableName;
    }
}
