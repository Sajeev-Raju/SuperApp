package com.example.idgeneration.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.example.idgeneration.controller..*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(* com.example.idgeneration.service..*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(* com.example.idgeneration.repository..*(..))")
    public void repositoryMethods() {}

    @Before("controllerMethods()")
    public void logBeforeController(JoinPoint joinPoint) {
        logger.info("Controller: {} - Starting request", joinPoint.getSignature().toShortString());
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterController(JoinPoint joinPoint, Object result) {
        logger.info("Controller: {} - Completed with result: {}", joinPoint.getSignature().toShortString(), result);
    }

    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        logger.error("Error in {}: {}", joinPoint.getSignature().toShortString(), exception.getMessage(), exception);
    }

    @Around("serviceMethods()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        logger.debug("Service: {} - Starting execution", methodName);
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.debug("Service: {} - Completed in {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Service: {} - Failed after {}ms with error: {}", methodName, duration, e.getMessage());
            throw e;
        }
    }

    @Around("repositoryMethods()")
    public Object logAroundRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        logger.trace("Repository: {} - Starting execution with args: {}", methodName, joinPoint.getArgs());
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.trace("Repository: {} - Completed in {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Repository: {} - Failed after {}ms with error: {}", methodName, duration, e.getMessage());
            throw e;
        }
    }
}