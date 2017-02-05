package org.caffy.districall.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理接口的实现
 * 方法名附带参数类型
 */
@SuppressWarnings("unused")
public class ImplementationFactory {
    private static final Logger logger = LoggerFactory.getLogger(ImplementationFactory.class);

    /**
     * 通过接口或者类型名称查询实现类
     *
     * @param interfaceName 接口或类型全名
     * @return 实现类的类型
     */
    public static Class queryImplementation(String interfaceName) throws ClassNotFoundException {
        ClassLoader classLoader = ImplementationFactory.class.getClassLoader();
        return classLoader.loadClass(interfaceName);
    }

    /**
     * 通过名称来获取方法
     *
     * @param interfaceName 接口名称
     * @param methodName    带参数表的方法名称
     * @return 方法
     */
    public static Method queryMethodByName(String interfaceName, String methodName) throws ClassNotFoundException {
        Implementation implementation = implementations.get(interfaceName);
        if (implementation != null) return implementation.methods.get(methodName);

        ClassLoader classLoader = ImplementationFactory.class.getClassLoader();
        Class<?> aClass = classLoader.loadClass(interfaceName);
        return queryMethodByName(aClass, methodName);
    }

    public static Method queryMethodByName(Class<?> cls, String methodName) {
        Implementation implementation = createOrGet(cls);
        return implementation.methods.get(methodName);
    }

    /**
     * 返回带参数表的方法名称
     *
     * @param method 方法
     * @return 名称
     */
    public static String getMethodName(Method method) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(method.getName());
        builder.append('(');
        Class<?>[] types = method.getParameterTypes();
        if (types != null && types.length > 0) {
            for (Class<?> aClass : types) {
                builder.append(aClass.getSimpleName()).append(',');
            }
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(')');
        return builder.toString();
    }

    /**
     * 注册特定接口的实现
     *
     * @param interfaceClasses    接口列表
     * @param implementationClass 实现
     */
    @SuppressWarnings("unused")
    public static void registerImplementation(Class<?> implementationClass, Class<?>... interfaceClasses) {
        // 如果这个类实现了远程接口
        Implementation implementation = createOrGet(implementationClass);
        assert interfaceClasses != null;
        for (Class<?> interfaceClass : interfaceClasses) {
            implementations.put(interfaceClass.getName(), implementation);
        }
    }

    public static void registerImplementation(Class<?> implementationClass) {
        Implementation implementation = createOrGet(implementationClass);
        registerImplementation(implementationClass, implementation);
    }

    private static void registerImplementation(Class<?> implementationClass, Implementation implementation) {
        Class<?>[] interfaces = implementationClass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            Implementation i = implementations.put(anInterface.getName(), implementation);
            if (i != null) logger.warn("{} 接口找到多个实现。", anInterface.getName());
        }

        Class<?> superclass = implementationClass.getSuperclass();
        if (superclass != null) {
            // 循环注册继承
            Implementation i = implementations.put(superclass.getName(), implementation);
            if (i != null) logger.warn("{} 继承类找到多个实现。", superclass.getName());
            registerImplementation(superclass, implementation);
        }
    }

    private static Implementation createOrGet(Class<?> implementationClass) {
        Implementation implementation = implementations.get(implementationClass.getName());
        if (implementation == null) {
            implementation = new Implementation();
            Method[] methods = implementationClass.getMethods();
            if (methods != null) for (Method m : methods) {
                String name = getMethodName(m);
                implementation.methods.put(name, m);
            }
            implementations.put(implementationClass.getName(), implementation);
        }
        return implementation;
    }

    // 用来索引方法
    private static class Implementation {
        Map<String, Method> methods = new TreeMap<String, Method>();
    }

    private static ConcurrentHashMap<String, Implementation> implementations
            = new ConcurrentHashMap<String, Implementation>();
}
