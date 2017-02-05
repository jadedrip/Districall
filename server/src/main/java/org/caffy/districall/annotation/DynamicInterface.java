package org.caffy.districall.annotation;

import java.lang.annotation.*;


/**
 * 指示接口是动态接口
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicInterface {
}
