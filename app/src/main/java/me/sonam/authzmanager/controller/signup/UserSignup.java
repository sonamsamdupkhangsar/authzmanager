package me.sonam.authzmanager.controller.signup;
import jakarta.validation.constraints.NotEmpty;

import java.util.Arrays;

public class UserSignup {
    @NotEmpty(message="firstName cannot be empty")
    private String firstName;
    private String lastName;
    @NotEmpty(message="email cannot be empty")
    private String email;
    @NotEmpty(message="Username cannot be empty")
    private String authenticationId;
    @NotEmpty(message="password cannot be empty")
    private char[] password;

    @Override
    public String toString() {
        return "UserSignup{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", authenticationId='" + authenticationId + '\'' +
                ", password=" + Arrays.toString(password) +
                '}';
    }

    public UserSignup() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthenticationId() {
        return authenticationId;
    }

    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}
