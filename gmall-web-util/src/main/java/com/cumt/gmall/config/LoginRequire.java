package com.cumt.gmall.config;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {

    //是否需要登陆,通过在控制器上加注解来
    boolean autoRedirect() default true;
}
