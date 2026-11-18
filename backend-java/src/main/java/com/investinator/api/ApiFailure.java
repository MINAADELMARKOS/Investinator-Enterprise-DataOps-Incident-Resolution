package com.investinator.api;
public final class ApiFailure extends RuntimeException {
    public final int status;
    public ApiFailure(int status,String message) {super(message);this.status=status;}
}
