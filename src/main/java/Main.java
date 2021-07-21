import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    //info.json
    /*{
        DelayTime:1000,
        TargetSize:1024,
        DownloadStation:"http",
        DownloadStationUserName:"",
        DownloadStationUserPassword:"",
        [{url:"http",
        cookie:"cookie"
        },
        ...
        ]
    }*/
    static double currentSize = 0;
    static ArrayList<String> torrentUrls = new ArrayList<>();
    static OkHttpClient okHttpClient;
    static int numberOfTorrents = 0;
    static String home = System.getProperty("user.dir");
    static MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    public static void main(String[] args){
        if(!Setting.readFromFile(home))
            return;
        if(Setting.proxyPort==0||Setting.proxyIP.isEmpty())
            okHttpClient = new OkHttpClient.Builder().build();
        else
            okHttpClient = new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Setting.proxyIP, Setting.proxyPort))).build();
        MyTimerTask myTimerTask = new MyTimerTask();
        Timer timer = new Timer();
        timer.schedule(myTimerTask,0,Setting.delayTime);
    }
    static class MyTimerTask extends TimerTask{
        @Override
        public void run() {
            for(WebSite webSite:Setting.webSites){
                searchForOneWebSite(webSite.getUrl(), webSite.getCookie() , webSite.getHost());
            }
            System.out.println("目前已发送种子："+numberOfTorrents+"，目前已使用容量："+takeTwoNumberAfterPoint(currentSize/1024/1024)+"GB/"+takeTwoNumberAfterPoint(Setting.targetSize/1024/1024)+"GB");
        }
    }
    private static int searchForOneWebSite(String url,String cookie,String host){
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Cookie", cookie)
                    .build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            String html = response.body().string();
            JXDocument underTest = JXDocument.create(html);
            response.close();
            call.cancel();
            String xpath = "//table[@class='torrents']/tbody/tr/html()";
            List<JXNode> torrents = underTest.selN(xpath);
            for(int i = 1;i<torrents.size();i++){
                xpath = "//table[@class='torrents']/tbody/tr["+(i+1)+"]/td[4]/allText()";
                String time = underTest.selNOne(xpath).toString();
                String[] timeWord = new String[]{"年","year","月","month","天","day","时","hour","時","分","min"};
                for(int ii = 0;ii<timeWord.length;ii++){
                    if(time.contains(timeWord[ii])&&ii<9)
                        break;
                    else if(time.contains(timeWord[ii])&&ii>8){
                        time = time.substring(0,time.indexOf(timeWord[ii]));
                        try{
                            if(Integer.parseInt(time)<=Setting.timeLimit){
                                xpath = "//table[@class='torrents']/tbody/tr["+(i+1)+"]/td[5]/allText()";
                                String sizeStr = underTest.selNOne(xpath).toString();
                                if(torrents.get(i).toString().contains("pro_free")){
                                    xpath = "//table[@class='torrents']/tbody/tr["+(i+1)+"]/td[2]/table/tbody/tr/td[5]/html()";
                                    String torrentUrl = underTest.selNOne(xpath).toString();
                                    torrentUrl = host + torrentUrl.substring(torrentUrl.indexOf("href=\"")+"href=\"".length(),torrentUrl.indexOf("\">"));
                                    if(!torrentUrls.contains(torrentUrl)){
                                        torrentUrls.add(torrentUrl);
                                        if(changeToKB(sizeStr)>Setting.singleSeedMaxSize){
                                            System.out.println("无法添加当前种子，当前种子大于设定最大单种子大小阈值！\n当前种子大小："+sizeStr+","+changeToKB(sizeStr));
                                            break;
                                        }else if((currentSize + changeToKB(sizeStr))<Setting.targetSize){
                                            System.out.println(torrentUrl);
                                            numberOfTorrents++;
                                            if(sendSeed(torrentUrl,cookie)){
                                                currentSize += changeToKB(sizeStr);
                                            }
                                        }else
                                            System.out.println("无法添加当前种子，添加当前种子后大于设定最大空间阈值！\n当前种子大小："+sizeStr+","+changeToKB(sizeStr));
                                    }
                                }
                            }
                        }catch (NumberFormatException e){
                            if(time.contains("< 1分")||time.contains("< 1min")){
                                xpath = "//table[@class='torrents']/tbody/tr["+(i+1)+"]/td[5]/allText()";
                                String sizeStr = underTest.selNOne(xpath).toString();
                                if(torrents.get(i).toString().contains("pro_free")){
                                    xpath = "//table[@class='torrents']/tbody/tr["+(i+1)+"]/td[2]/table/tbody/tr/td[5]/html()";
                                    String torrentUrl = underTest.selNOne(xpath).toString();
                                    torrentUrl = host + torrentUrl.substring(torrentUrl.indexOf("href=\"")+"href=\"".length(),torrentUrl.indexOf("\">"));
                                    if(!torrentUrls.contains(torrentUrl)){
                                        torrentUrls.add(torrentUrl);
                                        if(changeToKB(sizeStr)>Setting.singleSeedMaxSize){
                                            System.out.println("无法添加当前种子，当前种子大于设定最大单种子大小阈值！\n当前种子大小："+sizeStr+","+changeToKB(sizeStr));
                                            break;
                                        }else if((currentSize + changeToKB(sizeStr))<Setting.targetSize){
                                            System.out.println(torrentUrl);
                                            if(sendSeed(torrentUrl,cookie)){
                                                System.out.println();
                                                currentSize += changeToKB(sizeStr);
                                            }
                                        }else
                                            System.out.println("无法添加当前种子，添加当前种子后大于设定最大空间阈值！\n当前种子大小："+sizeStr+","+changeToKB(sizeStr));
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numberOfTorrents;
    }
    private static double changeToKB(String str){
        String[] sizeStr = str.split(" ");
        double size = Double.parseDouble(sizeStr[0]);
        switch(sizeStr[1]){
            case "TB":
                size *= 1024;
            case "GB":
                size *= 1024;
            case "MB":
                size *= 1024;
        }
        return size;
    }
    private static boolean sendSeed(String url,String cookie){
        try{
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Cookie",cookie)
                    .build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            byte[] buffer = response.body().bytes();
            buffer = Base64Encoder.ByteBase64Encoder(buffer);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method","torrent-add");
            jsonObject.put("tag","");
            JSONObject torrent = new JSONObject();
            torrent.put("pause","false");
            torrent.put("metainfo", new String(buffer));
            jsonObject.put("arguments",torrent);
            response.close();
            call.cancel();
            RequestBody requestBody = RequestBody.create(JSON, jsonObject.toJSONString());
            request = new Request.Builder()
                    .url(Setting.downloadStation+"/transmission/rpc")
                    .method("POST",requestBody)
                    .addHeader("X-Transmission-Session-Id",Setting.sessionId)
                    .addHeader("Authorization",Setting.authorization)
                    .build();
            call = okHttpClient.newCall(request);
            response = call.execute();
            if(response.code()==409){
                Setting.sessionId = response.header("X-Transmission-Session-Id");
                response.close();
                call.cancel();
                request = new Request.Builder()
                        .url(Setting.downloadStation+"/transmission/rpc")
                        .method("POST",requestBody)
                        .addHeader("X-Transmission-Session-Id",Setting.sessionId)
                        .addHeader("Authorization",Setting.authorization)
                        .build();
                call = okHttpClient.newCall(request);
                response = call.execute();
                call.cancel();
            }
            if(response.code()==200){
                System.out.println("发送种子请求成功，返回值：\n"+JSONObject.parseObject(response.body().string()).toJSONString());
                response.close();
                call.cancel();
                return true;
            }else {
                response.close();
                call.cancel();
                return false;
            }
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            System.out.println("发送种子超时！");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("发生未知错误！");
            return false;
        }
    }
    public static String takeTwoNumberAfterPoint(Double d){
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(d);
    }
}
