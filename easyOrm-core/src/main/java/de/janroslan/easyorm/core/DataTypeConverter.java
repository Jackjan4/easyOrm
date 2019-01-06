package de.janroslan.easyorm.core;

import de.janroslan.jputils.reflection.ReflectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DataTypeConverter {


    public static ColumnDefinition.DataType JavaToSql(Class c) {

        if(c == int.class || c == Integer.class || c == byte.class || c == Byte.class || c == boolean.class || c == Boolean.class) {
            return ColumnDefinition.DataType.INTEGER;
        }
        if(c == String.class) {
            return ColumnDefinition.DataType.TEXT;
        }
        if(c == double.class || c == float.class ||c == Double.class || c == Float.class) {
            return ColumnDefinition.DataType.REAL;
        }
        if(c == List.class) {
            return ColumnDefinition.DataType.TEXT;
        }
        if (c == LocalDate.class || c == LocalDateTime.class) {
            return ColumnDefinition.DataType.TEXT;
        }

        return null;
    }


    public static boolean needsJsonConversion(Object obj) {
        Class c = obj.getClass();

        if (ReflectionUtils.isPrimiteOrWrapper(obj) || c == String.class){
            return false;
        }

        return true;
    }

    public static String LocalDateTimeToSqlString(LocalDateTime ldt) {
        return null;
    }


    public static LocalDateTime stringToLocalDateTime(CharSequence strDate) {
        return LocalDateTime.parse(strDate);
    }

    public static boolean intToBool(int bool) {
        return bool != 0;
    }

    /**
     * Converts values from sql to their java types
     * @param val
     * @return
     */
    public static Object sqlToJavaConversion(Class destination, Object val) {


        if (destination == boolean.class || destination == Boolean.class) {
            return intToBool((int)val);
        }
        if (destination == LocalDateTime.class) {
            return stringToLocalDateTime((CharSequence)val);
        }

        return val;
    }

}
