package com.example.tictactoe.validator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = DistinctPlayerNamesValidator.class)
public @interface DistinctPlayerNames {
    String message() default "Player names must be different";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}