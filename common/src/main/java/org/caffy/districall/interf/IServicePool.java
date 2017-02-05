package org.caffy.districall.interf;

import org.caffy.districall.impl.Services;

public interface IServicePool {
    /**
     * 通过名称查找支持的服务器组
     */
    Services queryService(String name) throws Exception;
}
