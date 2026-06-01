package me.sonam.authzmanager.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import me.sonam.authzmanager.controller.signup.UserSignup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserSignupValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    public void firstNameCanBeOneCharacter() {
        UserSignup userSignup = new UserSignup("J", "", "j@example.com",
                "j@example.com", null, false, null);

        Set<ConstraintViolation<UserSignup>> violations = validator.validate(userSignup);

        assertThat(violations).noneMatch(violation -> "firstName".equals(violation.getPropertyPath().toString()));
    }

    @Test
    public void firstNameCannotBeBlank() {
        UserSignup userSignup = new UserSignup("", "", "j@example.com",
                "j@example.com", null, false, null);

        Set<ConstraintViolation<UserSignup>> violations = validator.validate(userSignup);

        assertThat(violations).anyMatch(violation -> "firstName".equals(violation.getPropertyPath().toString()));
    }
}
