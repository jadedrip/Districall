package org.caffy.districall.utils;

import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 利用一致性hash，计算得到主键对应的服务
 *
 * @param <T> 服务对象的类型
 */
@SuppressWarnings("unused")
public class ConsistentHash<T> {
    // 虚拟节点个数
    private int numberOfReplicas = 32;
    // 建立有序的map
    private SortedMap<Integer, T> circle = new TreeMap<Integer, T>();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Random random = new Random();

    public ConsistentHash() {
    }

    public ConsistentHash(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public ConsistentHash(Collection<T> nodes) {
        assign(nodes);
    }

    public ConsistentHash(int numberOfReplicas, Collection<T> nodes) throws NoSuchAlgorithmException {
        this.numberOfReplicas = numberOfReplicas;
        assign(nodes);
    }

    /**
     * 清空列表
     */
    public void clear() {
        readWriteLock.writeLock().lock();
        circle.clear();
        readWriteLock.writeLock().unlock();
    }

    /**
     * map中添加服务器节点
     *
     * @param node 待添加的节点( 必须有合适的 toString 值）
     */
    public void add(T node) {
        int key = node.hashCode();

        // h = 31 * h + val[i];

        readWriteLock.writeLock().lock();
        // 虚拟节点所在的hash处，存放对应的实际的节点服务器
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(key, node);
            key = (key << 4) ^ (key >> 28) ^ (i + 1);
        }
        readWriteLock.writeLock().unlock();
    }

    /**
     * map中添加服务器节点
     *
     * @param special 指定 Key
     * @param node    待添加的节点( 必须有合适的 toString 值）
     */
    public void add(Object special, T node) {
        int key = special.hashCode();
        readWriteLock.writeLock().lock();
        // 虚拟节点所在的hash处，存放对应的实际的节点服务器
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(key, node);
            key = (key << 4) ^ (key >> 28) ^ (i + 1);
        }
        readWriteLock.writeLock().unlock();
    }

    /**
     * 初始化服务器节点
     *
     * @param nodes 待添加的节点列表( 注意，节电必须有合适的 toString 值）
     */
    public void assign(Collection<T> nodes) {
        SortedMap<Integer, T> tmpCircle = new TreeMap<Integer, T>();
        if (nodes != null) {
            for (T node : nodes) {
                int key = node.hashCode();
                // 虚拟节点所在的hash处，存放对应的实际的节点服务器
                for (int i = 0; i < numberOfReplicas; i++) {
                    tmpCircle.put(key, node);
                    key = (key << 4) ^ (key >> 28) ^ (i + 1);
                }
            }
        }
        readWriteLock.writeLock().lock();
        circle = tmpCircle;
        readWriteLock.writeLock().unlock();
    }

    /**
     * map中移除服务器节点
     *
     * @param node 要移除的节点, 或者 Key
     */
    public void remove(T node) {
        int key = node.hashCode();
        readWriteLock.writeLock().lock();
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(key);
            key = (key << 4) ^ (key >> 28) ^ (i + 1);
        }
        readWriteLock.writeLock().unlock();
    }

    /**
     * 根据对象的key值，映射到hash表中，得到与对象hash值最近的服务器，就是对象待存放的服务器
     *
     * @param key 主键（需要合理支持 hashCode() 方法）如果为空会返回随机节点
     * @return 服务对象
     */
    public T get(Object key) {
        // 得到对象的hash值，根据该hash值找hash值最接近的服务器
        int hash = key == null ? (int) (Thread.currentThread().getId() ^ random.nextInt()) : key.hashCode();

        readWriteLock.readLock().lock();
        if (circle.isEmpty()) {
            readWriteLock.readLock().unlock();
            return null;
        }
        // 以下为核心部分，寻找与上面hash最近的hash指向的服务器
        // 如果hash表circle中没有该hash值

        // tailMap为大于该hash值的circle的部分
        SortedMap<Integer, T> tailMap = circle.tailMap(hash);
        // tailMap.isEmpty()表示没有大于该hash的hash值
        // 如果没有大于该hash的hash值，那么从circle头开始找第一个；如果有大于该hash值得hash，那么就是第一个大于该hash值的hash为服务器
        // 既逻辑上构成一个环，如果达到最后，则从头开始
        hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        T t = circle.get(hash);
        readWriteLock.readLock().unlock();
        return t;
    }

     /**
     * 随机获取一个服务
     */
    public T getRandomOne() {
        return get(null);
    }
}
