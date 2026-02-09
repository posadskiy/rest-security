package com.posadskiy.restsecurity.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as public (no security check).
 * When a class has @Security, methods with @Public skip the security check.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Public {
}
