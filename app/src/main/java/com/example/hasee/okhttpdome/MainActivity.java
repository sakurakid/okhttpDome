package com.example.hasee.okhttpdome;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button syncGet;
    private Button asyncget;
    private Button post;
    private Button fileDowmload;
    private TextView textView;
    private String result;

    private static OkHttpClient client = new OkHttpClient();

    /**
     * 设置连接超时的设定，在静态方法内，构造方法被调用时候就已经被激活
     * @param savedInstanceState
     */
    static {
        client.newBuilder().connectTimeout(10, TimeUnit.SECONDS);
        client.newBuilder().readTimeout(10,TimeUnit.SECONDS);
        client.newBuilder().writeTimeout(10,TimeUnit.SECONDS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        initListener();
    }
    /**
     * 事件监听
     */
    private void initListener(){
        syncGet.setOnClickListener(this);
        asyncget.setOnClickListener(this);
        post.setOnClickListener(this);
        fileDowmload.setOnClickListener(this);
    }
    /**
     * 初始化布局
     */
    private void initialize(){
        syncGet = (Button)findViewById(R.id.btn_1);
        asyncget = (Button)findViewById(R.id.btn_2);
        post = (Button)findViewById(R.id.btn_3);
        fileDowmload = (Button)findViewById(R.id.btn_4);
        textView = (TextView)findViewById(R.id.iv_show);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_1:
               initSyncData();
               // OkHttpManager.getSync("http://www.baidu.com");
//
//                String s = OkHttpManager.getSyncString("http://www.baidu.com");
//                Log.d("233",s);
//               new Thread(new Runnable() {
//                   @Override
//                   public void run() {
//                       textView.setText(OkHttpManager.getSyncString("http://www.baidu.com"));
//                   }
//               }).start();
//                Log.d("233","122");
                break;
            case R.id.btn_2:
//                initAsyncGet();
                OkHttpManager.getAsync("http://www.baidu.com", new OkHttpManager.DataCallBack() {
                    @Override
                    public void requestFailure(Request request, IOException e) {

                    }
                    @Override
                    public void requestSuccess(String result) throws Exception {
                         textView.setText(result);
                    }
                });
                break;
            case R.id.btn_3:
                initPost();
                break;
            case R.id.btn_4:
                downLoadFile();
                break;
        }
    }
    /**
     * geti请求同步
     */
    private void initSyncData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Request request = new Request.Builder().url("http://www.baidu.com").build();
                    Response response = client.newCall(request).execute();
                    result = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(result);
                            Log.d("233","sync");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 异步请求
     */
    private void initAsyncGet(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder().url("http://www.baidu.com").build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // call   是一个接口，  是一个准备好的可以执行的request  可以取消，对位一个请求对象，只能单个请求
                         Log.d("233","请求失败");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        /**
                         * 通过拿到response这个响应请求，然后通过body().string(),拿到请求到的数据
                         *这里最好用string()  而不要用toString（）
                         * toString（）每个类都有的，是把对象转换为字符串
                         * string（）是把流转为字符串
                         */
                        result = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(result);
                            }
                        });

                    }
                });
            }
        }).start();
    }

    /**
     * 表单提交
     */
    private void initPost(){
        String url = "http://112.124.22.238:8081/course_api/banner/query";
        FormBody formBody = new FormBody.Builder()
                .add("type","1")
                .build();
        final Request request = new Request.Builder().url(url)
                .post(formBody).build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("成功");
                            }
                        });

                    }
                });
            }
        }).start();
    }

    /**
     * 文件下载
     */
    private void downLoadFile(){
        String url = "http://www.0551fangchan.com/images/keupload/20120917171535_49309.jpg";
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //请求的结果转换成字节流
                InputStream inputStream = response.body().byteStream();
                /**
                 * 在这里要加上权限   在mainfests文件中
                 * <uses-permission android:name="android.permission.INTERNET"/>
                 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
                 */
                //输出流
                FileOutputStream fileOutputStream = new FileOutputStream(new File("logo.jpg"));
                //定义一个字节数组
                byte[] buffer = new byte[2048];
                int len = 0;
                while((len =inputStream.read(buffer))!=-1){
                    //写出文件
                    fileOutputStream.write(buffer,0,len);
                }
                //关闭输出流
                fileOutputStream.flush();
                Log.d("233","下载成功");
            }
        });
    }
}
