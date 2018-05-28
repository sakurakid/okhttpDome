package com.example.hasee.okhttpdome;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 封装okhttp
 */
public class OkHttpManager {
    /**
     * 静态实例
     */
    private static OkHttpManager sokHttpManager;
    /**
     * okhttpclient实例
     */
    private OkHttpClient mClient;
    /**
     * 请求数据必须在子线程里面，所以用handler
     */
    private Handler mHandler;

    /**
     * 构造方法
     */
    private OkHttpManager() {
        mClient = new OkHttpClient();
        /**
         * 直接设置超时这些
         */
        mClient.newBuilder().connectTimeout(10, TimeUnit.SECONDS);
        mClient.newBuilder().readTimeout(10, TimeUnit.SECONDS);
        mClient.newBuilder().writeTimeout(10, TimeUnit.SECONDS);
        /**
         * 初始化handler
         */
        mHandler = new Handler(Looper.getMainLooper());
    }
    /**
     * 单例模式
     */
    public static OkHttpManager getInstance(){
        if (sokHttpManager == null){
            sokHttpManager = new OkHttpManager();
        }
        return sokHttpManager;
    }
    //-----------------同步请求-----------------------------------------
    /**
     * 对外提供get方法,
     */
    public static Response getSync(String url){
        //通过实例获取调用内部的方法
        return sokHttpManager.inner_getSync(url);
    }
    /**
     * get的内部的方法
     */
    private Response inner_getSync(String url){
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        try {
            response = mClient.newCall(request).execute();
        }catch (IOException e){
            e.printStackTrace();
        }
        return response;
    }
    /**
     * 对外获取同步的string方法
     */
    public static String getSyncString(String url){
        return sokHttpManager.inner_getSynString(url);
    }
    /**
     * 同步的方法
     */
    private String inner_getSynString(String url){
        String result = null;
        try {
            //结果转换为字符串
            result = inner_getSync(url).body().string();
        }catch (IOException e){
            e.printStackTrace();
        }
        return result;
    }
    //------------------异步请求数据-------------------------------------------------

    /**
     * 数据回调接口
     */
    public interface DataCallBack{
        void requestFailure(Request request,IOException e);
        void requestSuccess(String result) throws Exception;
    }
    /**
     * 分发失败的时候调用
     *
     * @param request
     * @param e
     * @param callBack
     */
    private void deliverDataFailure(final Request request, final IOException e, final DataCallBack callBack) {
        /**
         * 在这里使用异步处理
         */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.requestFailure(request, e);
                }
            }
        });
    }

    /**
     * 分发成功的时候调用
     *
     * @param result
     * @param callBack
     */
    private void deliverDataSuccess(final String result, final DataCallBack callBack) {
        /**
         * 在这里使用异步线程处理
         */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    try {
                        callBack.requestSuccess(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    //-------------------------异步的方式请求数据--------------------------
    public static void getAsync(String url, DataCallBack callBack) {
        getInstance().inner_getAsync(url, callBack);
    }

    /**
     * 内部逻辑请求的方法
     *
     * @param url
     * @param callBack
     * @return
     */
    private void inner_getAsync(String url, final DataCallBack callBack) {
        final Request request = new Request.Builder().url(url).build();

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverDataFailure(request, e, callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = null;
                try {
                    result = response.body().string();
                } catch (IOException e) {
                    deliverDataFailure(request, e, callBack);
                }
                deliverDataSuccess(result, callBack);
            }
        });
    }


    /**
     * 对外的接口
     * @param url
     * @param callBack
     */
    public static void getAsync(String url,Map<String, String> params,DataCallBack callBack){
        getInstance().inner_postAsync(url,params,callBack);
    }
    private void inner_postAsync(String url, Map<String,String> params, final DataCallBack callBack){
        RequestBody requestBody = null;
        if (params==null){
            params = new HashMap<>();
        }

        FormBody.Builder builder = new FormBody.Builder();

        //对参加的参数进行遍历
        for(Map.Entry<String,String>map:params.entrySet()){
            String key = map.getKey().toString();
            String value = null;
            //判断值是否为空
            if (map.getValue()==null){
                value = "";
            }else {
                value = map.getValue();
            }
            //添加到formbody
            builder.add(key,value);
        }
        requestBody = builder.build();
        //返回结果
        final Request request = new Request.Builder().url(url).post(requestBody).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverDataFailure(request,e,callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                deliverDataSuccess(result,callBack);

            }
        });
    }
    //-----------------------文件下载---------------------------------------------------
    public static void downloadAsync(String url,String desDir,DataCallBack callBack){
        getInstance().inner_downloadAsync(url,desDir,callBack);
    }

    /**
     *
     * @param url  下载地址
     * @param desDir  目标地址
     * @param callBack
     */
    private void inner_downloadAsync(final String url,final String desDir,final DataCallBack callBack){
        final Request request = new Request.Builder().url(url).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverDataFailure(request,e,callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                FileInputStream fileInputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    File file = new File(desDir,getFileName(url));
                    inputStream = response.body().byteStream();
                    fileInputStream = new FileInputStream(file);
                    int len  = 0;
                    byte[] bytes = new byte[2048];
                    //循环读取数据
                    while ((len = inputStream.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, len);
                    }
                    //关闭文件输出流
                    fileOutputStream.flush();
                    //调用分发数据成功的方法
                    deliverDataSuccess(file.getAbsolutePath(), callBack);
                }catch (IOException e){
                    //失败后
                    deliverDataFailure(request,e,callBack);
                    e.printStackTrace();
                }finally {
                    if (inputStream!=null){
                        inputStream.close();
                    }
                    if (fileOutputStream!=null){
                        fileOutputStream.close();
                    }
                }

            }
        });
    }
    /**
     * 根据文件url获取文件的路径名字
     *
     * @param url
     * @return
     */
    private String getFileName(String url) {
        int separatorIndex = url.lastIndexOf("/");
        String path = (separatorIndex < 0) ? url : url.substring(separatorIndex + 1, url.length());
        return path;
    }

}
