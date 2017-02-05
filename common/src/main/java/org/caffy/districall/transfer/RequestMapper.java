package org.caffy.districall.transfer;

import org.caffy.districall.interf.ICallback;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求映射
 */
public class RequestMapper {
    public static class Returned {
        public Class<?> type = null;
        public ICallback<?> callback;

        public Returned(Class<?> type, ICallback<?> callback) {
            this.type = type;
            this.callback = callback;
        }
    }

    static AtomicLong atomicLong = new AtomicLong((long) new SecureRandom().nextInt() << 32);
    static Map<Long, Returned> methodMap = new ConcurrentHashMap<Long, Returned>();

    public static long make(Class<?> type, ICallback<?> callback) {
        long id = atomicLong.incrementAndGet();
        methodMap.put(id, new Returned(type, callback));
        return id;
    }

    public static Returned pop(long id) {
        return methodMap.remove(id);
    }
}
