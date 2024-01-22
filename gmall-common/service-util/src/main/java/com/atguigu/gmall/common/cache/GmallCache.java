package com.atguigu.gmall.common.cache;

import java.lang.annotation.*;

/**
 * 定义一个注解用于给方法加分布式锁
 * @author atguigu-mqx
 */
@Target({ElementType.METHOD}) //表示该注解作用目标为方法
@Retention(RetentionPolicy.RUNTIME) // 表示该注解在运行时生效
@Inherited
@Documented
public @interface GmallCache {

    //  目的用这个前缀要想组成 缓存的key！如sku:skuId —— 锁的前缀
    String prefix() default "cache:";

}
