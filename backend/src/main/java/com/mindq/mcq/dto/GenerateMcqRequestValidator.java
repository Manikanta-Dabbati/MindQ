package com.mindq.mcq.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class GenerateMcqRequestValidator implements ConstraintValidator<ValidGenerateMcqRequest, GenerateMcqRequest> {

    @Override
    public boolean isValid(GenerateMcqRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean hasMaterial = request.getMaterialId() != null;
        boolean hasPrompt = request.getPrompt() != null && !request.getPrompt().isBlank();

        if (!hasMaterial && !hasPrompt) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Either materialId or prompt must be provided")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
