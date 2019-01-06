module de.janroslan.easyOrm.core {
    requires java.sql;
    requires de.janroslan.jputils.collections;
    requires de.janroslan.jputils.reflection;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires de.janroslan.easyOrm.annotations;


    exports de.janroslan.easyorm.core;
    exports de.janroslan.easyorm.utils;
}