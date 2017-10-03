package com.exactprosystems.jf.api.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginFieldDescription {
    String parameter() default "";
    String description() default "";
    String example() default "";
}