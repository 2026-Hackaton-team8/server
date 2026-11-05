package org.study.jibeo.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 한국 전화번호 형식 (010-1234-5678, 062-123-4567, 하이픈 생략 가능). null 은 통과하므로 필수면 @NotBlank 와 함께 쓴다.
 */
@Pattern(regexp = "^0\\d{1,2}-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    String message() default "전화번호 형식이 올바르지 않습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
