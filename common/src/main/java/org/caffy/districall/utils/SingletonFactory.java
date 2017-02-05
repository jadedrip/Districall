package org.caffy.districall.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个简单的单例工厂
 */
@SuppressWarnings("unused")
public class SingletonFactory {
    private static ConcurrentHashMap<String, Object> classes = new ConcurrentHashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public static <T> T query(Class<T> tClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        Object o = classes.get(tClass.getName());
        if (o == null) {
            throw new NoSuchMethodException();
        }
        return (T) o;
    }

    public static Object query(String name){
        return classes.get(name);
    }

    /**
     * 设置一个单例
     * @param singleton 单例对象
     */
    public static void setSingleton(Class<?> c, Object singleton) {
        classes.put( c.getName(), singleton );
    }
}
