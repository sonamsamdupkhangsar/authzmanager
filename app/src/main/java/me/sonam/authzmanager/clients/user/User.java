package me.sonam.authzmanager.clients.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public class User {
    private static final Logger LOG = LoggerFactory.getLogger(User.class);

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String authenticationId;
    private Boolean active;
    private Boolean userAuthAccountCreated;
    private OrganizationChoice organizationChoice = new OrganizationChoice();
    private Boolean searchable;

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public User(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public User(UUID id) {
        this.id = id;
    }
    @Override
    public boolean equals(Object o) {
        LOG.info("equlas called");
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        LOG.info("hashcode called");
        return Objects.hash(id);
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
                ", searchable=" + searchable +
                '}';
    }
}