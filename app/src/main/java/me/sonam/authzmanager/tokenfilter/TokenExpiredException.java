package me.sonam.authzmanager.tokenfilter;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super (message);
    }
}
