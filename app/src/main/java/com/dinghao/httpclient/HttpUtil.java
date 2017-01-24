package com.dinghao.httpclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by li on 2017/1/18.
 */

public class HttpUtil {
    static int TIMEOUIT = 300 * 1000; // 超时时间 5m
    static String BOUNDARY = UUID.randomUUID().toString();
    static String PREFIX = "--", LINEND = "\r\n";
    static String MULTIPART_FROM_DATA = "multipart/form-data";
    static String CHARSET = "UTF-8";
    static OutputStream outputStream = null;
    static InputStream inputStream = null;
    static HttpURLConnection conn = null;
    public static InputStream get(String url){

        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUIT);
            conn.setReadTimeout(TIMEOUIT); // 缓存的最长时间
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Charset", CHARSET);
            conn.connect();
            if(conn.getResponseCode()==200){
                inputStream = conn.getInputStream();
                return inputStream;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(conn!=null){
                    conn.disconnect();
                }
                if(inputStream!=null){
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static InputStream post(String url,HashMap<String, String> params){

        try {
            StringBuilder buf = new StringBuilder();
            Set<Map.Entry<String, String>> entries;
            // 如果存在参数，则放在HTTP请求体，形如name=aaa&age=10
            if (params != null && !params.isEmpty()) {
                entries = params.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    buf.append(entry.getKey()).append("=")
                            .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                            .append("&");
                }
                buf.deleteCharAt(buf.length() - 1);
            }
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUIT);
            conn.setReadTimeout(TIMEOUIT); // 缓存的最长时间
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Charset", CHARSET);
            OutputStream out = conn.getOutputStream();
            out.write(buf.toString().getBytes("UTF-8"));
            conn.connect();
            if(conn.getResponseCode()==200){
                inputStream = conn.getInputStream();
                return inputStream;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(conn!=null){
                    conn.disconnect();
                }
                if(inputStream!=null){
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static boolean upLoadFiles(String url,HashMap<String, String> params,Map<File, String> files,Progress progress,ResponseState response){
        try {
            StringBuilder buf = new StringBuilder();
            Set<Map.Entry<String, String>> entries;
            // 如果存在参数，则放在HTTP请求体，形如name=aaa&age=10
            if (params != null && !params.isEmpty()) {
                entries = params.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    buf.append(entry.getKey()).append("=")
                            .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                            .append("&");
                }
                buf.deleteCharAt(buf.length() - 1);
            }
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUIT);
            conn.setReadTimeout(TIMEOUIT); // 缓存的最长时间
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Charset", CHARSET);
            conn.connect();
            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.write(buf.toString().getBytes());
            //write file
            byte[] data = new byte[4*1024];
            for (Map.Entry<File, String> entry : files.entrySet()) {
                int rbytes;
                long written = 0;
                File file = entry.getKey();
                String fileName = entry.getValue();
                outStream.writeBytes(PREFIX + BOUNDARY + LINEND);
                outStream.writeBytes("Content-Disposition: form-data; "
                        + "name=\""+fileName+"\";filename=\""+file.getName()+"\""
                        + LINEND);
                outStream.writeBytes("Content-Type: application/octet-stream"+ LINEND);
                outStream.writeBytes("Content-Transfer-Encoding: binary" + LINEND);
                outStream.writeBytes(LINEND);
                FileInputStream fis = new FileInputStream(file);
                while ((rbytes = fis.read(data)) != -1) {
                    outStream.write(data, 0, rbytes);
                    written += rbytes;
                    //同步更新数据
                    if(progress != null) {
                        progress.updateSize(fileName, written);
                    }
                }
                outStream.writeBytes(LINEND);
            }
            // 请求结束标志
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
            outStream.write(end_data);
            outStream.flush();
            // 得到响应码
            // 得到响应码
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();
                StringBuffer sb = new StringBuffer();
                int rbytes;
                while ((rbytes = inputStream.read(data)) > 0) {  /* != -1 */
                    sb.append(new String(data, 0, rbytes));
                }
                int errorNo = 0;
                try {
                    JSONObject json = new JSONObject(sb.toString());
                    errorNo = Integer.parseInt(json.optString("errorNo"));
                    if (errorNo==0) {
                        response.onSuccess(sb.toString());
                        return true;
                    }
                }catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                response.onFailure(errorNo,sb.toString());
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(conn!=null){
                    conn.disconnect();
                }
                if(inputStream!=null){
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public interface ResponseState{
        boolean onSuccess(String response);
        boolean onFailure(int statusCode, String response);
    }
    public interface Progress{
        void updateSize(long size);
        void updateSize(String fireName,long size);
    }
    public static boolean downloadFile(String url, String filePath, Progress progress){
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUIT);
            conn.setReadTimeout(TIMEOUIT); // 缓存的最长时间
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Charset", CHARSET);
            conn.connect();
            if(conn.getResponseCode()==200){
                inputStream = conn.getInputStream();
                File file = new File(filePath);
                File dir = new File(file.getParent());
                if(!dir.exists()) dir.mkdirs();
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[4*1024];
                int len;
                int size = 0;
                while ((len = inputStream.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    fos.flush();
                    size += len;
                    //同步更新数据
                    if(progress != null) {
                        progress.updateSize(size);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(conn!=null){
                    conn.disconnect();
                }
                if(inputStream!=null){
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
