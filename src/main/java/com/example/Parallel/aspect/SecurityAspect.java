package com.example.Parallel.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Aspect
@Component
public class SecurityAspect {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Before("execution(* com.example.Parallel.service.AuthService.register(..))"
            + " && args(request, ..)")
    public void beforeRegister(Object request) {
        String email = extractEmail(request);
        audit("REGISTER", email, "ATTEMPT");
    }

    @AfterReturning("execution(* com.example.Parallel.service.AuthService.register(..))")
    public void afterRegisterSuccess(JoinPoint jp) {
        String email = extractEmail(jp.getArgs()[0]);
        audit("REGISTER", email, "SUCCESS");
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.Parallel.service.AuthService.register(..))",
            throwing = "ex")
    public void afterRegisterFailed(JoinPoint jp, Throwable ex) {
        String email = extractEmail(jp.getArgs()[0]);
        audit("REGISTER", email, "FAILED  → " + ex.getMessage());
    }


    @Before("execution(* com.example.Parallel.service.AuthService.login(..))"
            + " && args(request, ..)")
    public void beforeLogin(Object request) {
        String email = extractEmail(request);
        audit("LOGIN", email, "ATTEMPT");
    }

    @AfterReturning("execution(* com.example.Parallel.service.AuthService.login(..))")
    public void afterLoginSuccess(JoinPoint jp) {
        String email = extractEmail(jp.getArgs()[0]);
        audit("LOGIN", email, "SUCCESS");
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.Parallel.service.AuthService.login(..))",
            throwing = "ex")
    public void afterLoginFailed(JoinPoint jp, Throwable ex) {
        String email = extractEmail(jp.getArgs()[0]);
        audit("LOGIN", email, "FAILED  → " + ex.getMessage());
    }


    @AfterReturning(
            pointcut = "execution(* com.example.Parallel.service.CartService.checkout*(..)) && args(email, ..)",
            returning = "result")
    public void afterCheckoutSuccess(String email, Object result) {
        audit("CHECKOUT", email, "SUCCESS");
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.Parallel.service.CartService.checkout*(..)) && args(email, ..)",
            throwing = "ex")
    public void afterCheckoutFailed(String email, Throwable ex) {
        audit("CHECKOUT", email, "FAILED  → " + ex.getMessage());
    }


    @AfterReturning(
            pointcut = "execution(* com.example.Parallel.service.PaymentService.processPayment(..))"
                    + " && args(request, email)",
            returning = "result")
    public void afterPaymentSuccess(Object request, String email, Object result) {
        String orderId = extractOrderId(request);
        audit("PAYMENT", "orderId=" + orderId + " by " + email, "SUCCESS");
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.Parallel.service.PaymentService.processPayment(..))"
                    + " && args(request, email)",
            throwing = "ex")
    public void afterPaymentFailed(Object request, String email, Throwable ex) {
        String orderId = extractOrderId(request);
        audit("PAYMENT", "orderId=" + orderId + " by " + email, "FAILED  → " + ex.getMessage());
    }


    @AfterReturning(
            pointcut = "execution(* com.example.Parallel.service.OrderService.cancelOrder(..))"
                    + " && args(orderId, email)",
            returning = "result")
    public void afterCancelSuccess(Integer orderId, String email, Object result) {
        audit("CANCEL", "orderId=" + orderId + " by " + email, "SUCCESS");
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.Parallel.service.OrderService.cancelOrder(..))"
                    + " && args(orderId, email)",
            throwing = "ex")
    public void afterCancelFailed(Integer orderId, String email, Throwable ex) {
        audit("CANCEL", "orderId=" + orderId + " by " + email, "FAILED  → " + ex.getMessage());
    }

    private void audit(String action, String subject, String outcome) {
        log.info("[Security] {} | {} | {} | {}",
                LocalDateTime.now().format(FMT),
                action, subject,
                outcome);
    }

    private String extractEmail(Object obj) {
        if (obj == null) return "unknown";
        try {
            var method = obj.getClass().getMethod("getEmail");
            Object email = method.invoke(obj);
            return email != null ? email.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }


    private String extractOrderId(Object obj) {
        if (obj == null) return "unknown";
        try {
            var method = obj.getClass().getMethod("getOrderId");
            Object id = method.invoke(obj);
            return id != null ? id.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
