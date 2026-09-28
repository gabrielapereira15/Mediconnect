package com.vegs.mediconnect.backoffice.schedule_time;

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
        validatedBy = ScheduleDateTimeUnique.ScheduleDateTimeUniqueValidator.class
)
public @interface ScheduleDateTimeUnique {

    String message() default "{Exists.scheduleTime.dateTime}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ScheduleDateTimeUniqueValidator implements ConstraintValidator<ScheduleDateTimeUnique, ScheduleDateTimeDTO> {

        private final ScheduleTimeService scheduleTimeService;

        public ScheduleDateTimeUniqueValidator(final ScheduleTimeService scheduleTimeService) {
            this.scheduleTimeService = scheduleTimeService;
        }

        @Override
        public boolean isValid(final ScheduleDateTimeDTO value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }

            return !scheduleTimeService.isScheduleDateTimeExist(value);
        }

    }

}
