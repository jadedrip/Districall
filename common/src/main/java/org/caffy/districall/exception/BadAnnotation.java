package org.caffy.districall.exception;

/**
 * 指示注解错误
 */
@SuppressWarnings("unused")
public class BadAnnotation extends Exception {
    public BadAnnotation() {
        super("Protocol not supported.");
    }

    public BadAnnotation(String message) {
        super(message);
    }

    public BadAnnotation(String message, Throwable cause) {
        super(message, cause);
    }

    public BadAnnotation(Throwable cause) {
        super("Protocol not supported.", cause);
    }

    public BadAnnotation(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
