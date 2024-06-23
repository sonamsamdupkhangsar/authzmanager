package me.sonam.authzmanager.tokenfilter;

public class UserNotAuthenticatedException extends RuntimeException {
    public UserNotAuthenticatedException(String message) {
        super (message);
    }
}
