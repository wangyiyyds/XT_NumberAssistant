package com.xianting.manage.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP 切面：统一记录 Service 实现层方法的入参、耗时与异常，
 * 业务代码无需重复编写日志逻辑。
 */
@Slf4j
@Aspect
@Component
public class ServiceLogAspect {

    @Pointcut("execution(* com.xianting.manage.service.impl..*.*(..))")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logAroundService(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        if (log.isDebugEnabled()) {
            log.debug("--> {} args={}", method, Arrays.toString(pjp.getArgs()));
        }
        try {
            Object result = pjp.proceed();
            log.info("<-- {} cost={}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("<-- {} exception={} cost={}ms", method, e.toString(), System.currentTimeMillis() - start);
            throw e;
        }
    }
}
