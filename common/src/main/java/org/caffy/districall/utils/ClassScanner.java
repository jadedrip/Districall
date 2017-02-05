package org.caffy.districall.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 包扫描工具
 */
@SuppressWarnings("unused")
public class ClassScanner {
    private static String[] paths = System.getProperty("java.class.path")
            .split(System.getProperty("path.separator"));

    public static ArrayList<Class> scan(String packagePath) {
        ArrayList<Class> classes = new ArrayList<Class>();
        String classPath = packagePath.replace(File.separatorChar, '.');
        for (String path : paths) {
            fillClasses(path, classPath, new File(path), classes);
        }
        return classes;
    }

    /**
     * 填充满足条件的class 填充到 classes
     *
     * @param file 类路径下的文件
     */
    private static void fillClasses(final String base, String packagePath, File file, ArrayList<Class> classes) {
        if (file.isDirectory()) {
            processDirectory(base, packagePath, file, classes);
        } else if (isClass(file.getName())) {
            processClassFile(base, packagePath, file, classes);
        } else if (isJarFile(file.getName())) {
            processJarFile(base, file, classes);
        }
    }

    /**
     * 处理如果为目录的情况,需要递归调用 fillClasses方法
     */
    private static void processDirectory(String base, String packagePath, File directory, ArrayList<Class> classes) {
        System.out.println("Scan directory:" + directory);
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return isClass(pathname.getName()) || pathname.isDirectory();
            }
        });

        if (files != null) for (File file : files)
            fillClasses(base, packagePath, file, classes);
    }

    /**
     * 处理为class文件的情况,填充满足条件的class 到 classes
     */
    private static void processClassFile(String base, String packagePath, File file, ArrayList<Class> classes) {
        String path = file.getAbsolutePath();
        assert path.startsWith(base);
        path = path.substring(base.length());
        if (path.length() < 2) return;
        if (path.charAt(0) == File.separatorChar)
            path = path.substring(1);

        final String filePathWithDot = path.replace(File.separator, ".");
        String className = filePathWithDot.replace(CLASS_EXT, "");
        fillClass(className, classes);
    }

    /**
     * 填充 class 到 哈希表
     */
    private static void fillClass(String className, List<Class> classes) {
        try {
            final Class clazz = Class.forName(className, false, ClassScanner.class.getClassLoader());
            if (clazz != null) classes.add(clazz);
        } catch (Throwable ignored) {
            // ignore this ex
        }
    }

    /**
     * 处理为jar文件的情况，填充满足条件的class 到 classes
     */
    private static void processJarFile(String base, File file, List<Class> classes) {
        try {
            for (ZipEntry entry : Collections.list(new ZipFile(file).entries())) {
                String name = entry.getName();
                if (isClass(name)) {
                    if (name.startsWith(base)) {
                        final String className = entry.getName().replace(File.separatorChar, '.').replace(CLASS_EXT, "");
                        fillClass(className, classes);
                    }
                }
            }
        } catch (Throwable ex) {
            // ignore this ex
        }
    }

    private static final String CLASS_EXT = ".class";
    private static final String JAR_FILE_EXT = ".jar";

    private static boolean isClass(String fileName) {
        return fileName.endsWith(CLASS_EXT);
    }

    private static boolean isJarFile(String fileName) {
        return fileName.endsWith(JAR_FILE_EXT);
    }

    public static void main(String[] args) {
        ArrayList<Class> scan = ClassScanner.scan("im.yixin");
        //ArrayList<Class> scan = ClassScanner.scanJar("log4j");
        for (Class aClass : scan) {
            System.out.println(aClass);
        }
    }
}
