package org.caffy.districall.beans;

import java.io.Serializable;

/**
 * 用来包装异常
 */
public class ExceptionWarp implements Serializable {
    private String type;
    private String message;

    public static ExceptionWarp warp( Throwable throwable ){
        ExceptionWarp warp = new ExceptionWarp();
        warp.setType( throwable.getClass().getName() );
        warp.setMessage( throwable.getMessage() );
        return warp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
