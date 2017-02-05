package org.caffy.districall.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Uri 处理工具
 */
public class UriUtils {
    /**
     * Uri 的参数部分解析（不考虑重复参数）
     * @param queryString 参数字符串（x=12&y=hab&z=2)
     * @return 参数表
     */
    public static Map<String, String> parseQuery(String queryString) {
        String[] queryStringSplit = queryString.split("&");
        Map<String, String> queryStringMap = new HashMap<String, String>(
                queryStringSplit.length);
        String[] queryStringParam;
        for (String qs : queryStringSplit) {
            queryStringParam = qs.split("=");
            queryStringMap.put(queryStringParam[0], queryStringParam[1]);
        }
        return queryStringMap;
    }
}
