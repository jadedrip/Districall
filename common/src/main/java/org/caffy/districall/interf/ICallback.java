package org.caffy.districall.interf;

/**
 * 回调函数
 */
public interface ICallback<T> {
    void apply(T v);

    void except(Throwable e);
}
