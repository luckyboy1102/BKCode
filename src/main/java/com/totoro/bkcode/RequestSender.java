package com.totoro.bkcode;

import com.squareup.okhttp.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Chen on 2015/6/2.
 */
public class RequestSender {

    private final OkHttpClient client;

    public RequestSender() {
        client = new OkHttpClient();
        client.setConnectTimeout(15, TimeUnit.SECONDS);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(cookieManager);
    }

    /**
     * 该不会开启异步线程。
     * @param request
     * @return
     */
    public Response execute(Request request) {
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 开启异步线程访问网络
     * @param url
     * @param responseCallback
     */
    public void enqueue(String url, Callback responseCallback) {
        client.newCall(getRequest(url, null)).enqueue(responseCallback);
    }

    public String execute(String url, Map<String, String> postParams) {
        Response response = execute(getRequest(url, postParams));
        String responseStr = null;
        if (response != null && response.isSuccessful()) {
            try {
                if (response.isRedirect()) {

                } else {
                    responseStr = response.body().string();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseStr;
    }

    public String execute(String url) {
        return execute(url, null);
    }

    public Request getRequest(String url, Map<String, String> params) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.header("Accept", "text/html");
        builder.header("Accept-Charset", "utf-8");
        builder.header("Accept-Encoding", "gzip, deflate");
        builder.header("Accept-Language", "zh-CN");
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko");

        if (params != null && params.size() > 0) {
            FormEncodingBuilder formBody = new FormEncodingBuilder();
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                formBody.add(entry.getKey(), entry.getValue());
            }
            builder.post(formBody.build());
        }

        return builder.build();
    }
}
