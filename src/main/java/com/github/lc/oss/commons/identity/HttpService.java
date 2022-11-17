package com.github.lc.oss.commons.identity;

import java.util.Map;

import com.github.lc.oss.commons.serialization.Jsonable;

public interface HttpService {

    void delete(String url, Map<String, String> headers);

    <T> T get(String url, Map<String, String> headers, Class<T> responseType);

    <T> T post(String url, Map<String, String> headers, Class<T> responseType, Jsonable requestBody);

    <T> T post(String url, Map<String, String> headers, Class<T> responseType, Jsonable requestBody, String privateKey);

    void put(String url, Map<String, String> headers, Jsonable requestBody);

}
