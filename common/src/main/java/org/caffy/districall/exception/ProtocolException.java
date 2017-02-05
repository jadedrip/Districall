package org.caffy.districall.exception;

import java.io.IOException;

/**
 * 协议异常（需要升级版本？）
 */
@SuppressWarnings("unused")
public class ProtocolException extends IOException {
    public ProtocolException() {
        super();
    }

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolException(Throwable cause) {
        super(cause);
    }
}
