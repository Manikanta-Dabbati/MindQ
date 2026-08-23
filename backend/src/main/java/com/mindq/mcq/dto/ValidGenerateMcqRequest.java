package com.mindq.mcq.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GenerateMcqRequestValidator.class)
@Documented
public @interface ValidGenerateMcqRequest {
    String message() default "Either materialId or prompt must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
