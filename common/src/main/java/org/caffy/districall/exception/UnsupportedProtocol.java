package org.caffy.districall.exception;

import java.io.IOException;

/**
 * 指示不支持服务端的协议（需要升级版本？）
 */
@SuppressWarnings("unused")
public class UnsupportedProtocol extends IOException {
    public UnsupportedProtocol() {
        super("Protocol not supported.");
    }

    public UnsupportedProtocol(String message) {
        super(message);
    }

    public UnsupportedProtocol(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedProtocol(Throwable cause) {
        super("Protocol not supported.", cause);
    }

}
