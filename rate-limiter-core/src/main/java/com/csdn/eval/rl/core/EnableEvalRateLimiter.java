package com.csdn.eval.rl.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({RateLimiterConfig.class})
public @interface EnableEvalRateLimiter {
}
