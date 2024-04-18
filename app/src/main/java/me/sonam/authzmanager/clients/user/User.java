package me.sonam.authzmanager.clients.user;

import java.util.Objects;
import java.util.UUID;

public class User {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String authenticationId;
    private Boolean active;
    private Boolean userAuthAccountCreated;
    private OrganizationChoice organizationChoice = new OrganizationChoice();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(firstName, user.firstName) &&
                Objects.equals(lastName, user.lastName) && Objects.equals(email, user.email) &&
                Objects.equals(authenticationId, user.authenticationId) && Objects.equals(active, user.active) &&
                Objects.equals(userAuthAccountCreated, user.userAuthAccountCreated) &&
                Objects.equals(organizationChoice, user.organizationChoice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, authenticationId, active, userAuthAccountCreated, organizationChoice);
    }

    public User() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getUserAuthAccountCreated() {
        return userAuthAccountCreated;
    }

    public void setUserAuthAccountCreated(Boolean userAuthAccountCreated) {
        this.userAuthAccountCreated = userAuthAccountCreated;
    }

    public OrganizationChoice getOrganizationChoice() {
        return organizationChoice;
    }

    public void setOrganizationChoice(OrganizationChoice organizationChoice) {
        this.organizationChoice = organizationChoice;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", authenticationId='" + authenticationId + '\'' +
                ", active=" + active +
                ", userAuthAccountCreated=" + userAuthAccountCreated +
                ", organizationChoice=" + organizationChoice +
                '}';
    }

}