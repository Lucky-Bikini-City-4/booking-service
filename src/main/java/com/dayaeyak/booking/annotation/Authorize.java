package com.dayaeyak.booking.annotation;



import com.dayaeyak.booking.common.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Authorize {

    UserRole[] roles() default {};

    boolean bypass() default false;
}
