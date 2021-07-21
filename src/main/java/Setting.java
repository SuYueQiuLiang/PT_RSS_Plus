import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Setting {
    public static boolean readFromFile(String home){
        File infoFile = new File(home+"/cache"+"/info.json");
        System.out.println(home+"/cache"+"/info.json");
        if(!infoFile.exists()){
            System.out.print("没有站点信息，请按教程配置站点信息！");
            return false;
        }
        try{
            FileInputStream fileInputStream = new FileInputStream(infoFile);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            JSONObject jsonObject = JSON.parseObject(new String(bytes));
            downloadStation = jsonObject.getString("DownloadStation");
            delayTime = jsonObject.getInteger("DelayTime");
            timeLimit = jsonObject.getInteger("TimeLimit");
            targetSize = jsonObject.getDouble("TargetSize")*1024*1024;
            singleSeedMaxSize = jsonObject.getDouble("SingleSeedMaxSize")*1024*1024;
            proxyIP = jsonObject.getString("ProxyIP");
            proxyPort = jsonObject.getInteger("ProxyPort");
            authorization = "Basic "+Base64Encoder.Base64Encoder(jsonObject.getString("DownloadStationUserName")+":"+jsonObject.getString("DownloadStationUserPassword"));
            JSONArray jsonArray = jsonObject.getJSONArray("WebSite");
            for(int i = 0;i<jsonArray.size();i++){
                webSites.add(new WebSite(jsonArray.getJSONObject(i).getString("url"),jsonArray.getJSONObject(i).getString("cookie"),jsonArray.getJSONObject(i).getString("host")));
            }
            System.out.println("读取配置文件成功！准备开始检索数据！");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("读取配置文件失败！请检查格式！");
            return false;
        }
    }
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
    public static int delayTime,timeLimit,proxyPort;;
    public static double targetSize,singleSeedMaxSize;
    public static String downloadStation,authorization,proxyIP;
    public static ArrayList<WebSite> webSites = new ArrayList<>();
    public static String sessionId = "";
}
