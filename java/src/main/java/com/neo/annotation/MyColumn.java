package com.neo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyColumn {

    //字段中文名
    String name() default "";
    //字段备注
    String memo() default "";
    //字段长度
    int length() default 1;
    //是否必须
    boolean isRequire() default true;
}
