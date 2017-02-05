package org.caffy.districall.exception;

/**
 * 远程异常
 */
public class RemoteException extends Exception {
    private String type;

    public String getType() {
        return type;
    }

    public RemoteException(String type) {
        this.type = type;
    }

    public RemoteException(String message, String type) {
        super(message);
        this.type = type;
    }

    public RemoteException(String message, Throwable cause, String type) {
        super(message, cause);
        this.type = type;
    }

    public RemoteException(Throwable cause, String type) {
        super(cause);
        this.type = type;
    }
}
