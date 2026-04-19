package com.netcloud.exceptions;

public class MongodbConnectionException extends Exception{
    public MongodbConnectionException(String exceptionStr)
    {
        super(exceptionStr);
    }
}
