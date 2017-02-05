package org.caffy.districall.interf;

/**
 * 对象工厂接口
 */
public interface IFactory<T> {
    T getObject( Object[] parameters );
}
