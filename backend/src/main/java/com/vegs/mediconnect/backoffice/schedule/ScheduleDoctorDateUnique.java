package com.vegs.mediconnect.backoffice.schedule;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;


/**
 * Validate that the id value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = ScheduleDoctorDateUnique.ScheduleDoctorDateUniqueValidator.class
)
public @interface ScheduleDoctorDateUnique {

    String message() default "{Exists.schedule.doctorDate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ScheduleDoctorDateUniqueValidator implements ConstraintValidator<ScheduleDoctorDateUnique, DoctorDateDTO> {

        private final ScheduleService scheduleService;

        public ScheduleDoctorDateUniqueValidator(final ScheduleService scheduleService) {
            this.scheduleService = scheduleService;
        }

        @Override
        public boolean isValid(final DoctorDateDTO value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }

            return !scheduleService.isDoctorDateExists(value);
        }

    }

}
