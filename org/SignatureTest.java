import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.xpath.internal.operations.String;

import org.json.*;

//java -Dfile.encoding=utf-8  SignatureTest
//javac -encoding utf-8 SignatureTest.java


public class SignatureTest {

	public static void main(String[] args) {
		HttpURLConnection connection=null;
		String APIUrl = "http://ptx.transportdata.tw/MOTC/v2/Rail/TRA/LiveBoard?$format=JSON";
	    
        String APPID = "71a1de694a034562a7356e8dce5c5791";
        
        String APPKey = "Oexh2v7Rpof5X2B22xLpRuZIscs";

        System.setProperty("file.encoding", "UTF-8");
        String xdate = getServerTime();
        String SignDate = "x-date: " + xdate;
        
        
        String Signature="";
		try {
			
			Signature = HMAC_SHA1.Signature(SignDate, APPKey);
		} catch (SignatureException e1) {
			
			e1.printStackTrace();
		}
		
		System.out.println("Signature :" + Signature);
        String sAuth = "hmac username=\"" + APPID + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"" + Signature + "\"";
        System.out.println(sAuth);
		   try{  
		      URL url=new URL(APIUrl);
		      connection=(HttpURLConnection)url.openConnection();
		      connection.setRequestMethod("GET");
		      connection.setRequestProperty("Authorization", sAuth);
		      connection.setRequestProperty("x-date", xdate);
		      connection.setRequestProperty("Accept-Encoding", "gzip");
		      connection.setDoInput(true);
		      connection.setDoOutput(true);
		      
		      
		      InputStream inputStream = connection.getInputStream();
	          ByteArrayOutputStream bao = new ByteArrayOutputStream();
		      byte[] buff = new byte[1024];
	          int bytesRead = 0;
	          while((bytesRead = inputStream.read(buff)) != -1) {
	             bao.write(buff, 0, bytesRead);
	          }
	          
	          
		      ByteArrayInputStream bais = new ByteArrayInputStream(bao.toByteArray());
		      GZIPInputStream gzis = new GZIPInputStream(bais);
		      InputStreamReader reader = new InputStreamReader(gzis);
		      BufferedReader in = new BufferedReader(reader);
		      
		      
		      String line, response="";
		      while ((line = in.readLine()) != null) {
		          response+=(line+"\n");
		      }
		      
		      Type RailStationListType = new TypeToken<ArrayList<RailStation>>(){}.getType();
		      Gson gsonReceiver = new Gson();
		      List<RailStation> obj = gsonReceiver.fromJson(response, RailStationListType);
			  //System.out.println(response);

			  JSONArray j;
			  JSONObject jj;
			  File file = new File("output.txt");
			  try {
				  j = new JSONArray(response);
				  String[] data_Unfinished = new String[j.length()];
				  FileWriter fw = new FileWriter(file);
				  String temp="",temp_2="";
				  int station = 1,station_tmp = 0;
				  int along_tmp = 0,inverse_tmp = 0; //順行計次,逆行計次
				  for (int a=0; a < j.length(); a++) {
					  data_Unfinished[a] = String.valueOf(j.get(a));
					  System.out.println(data_Unfinished[a] + "\n");
					  fw.write(data_Unfinished[a] + "\n");
				  }
				  fw.flush();
				  fw.close();
				  System.out.println("共抓到"+j.length()+"筆資料");
				  for(int a = 0; a < j.length() ; a++){
					  jj = new JSONObject(data_Unfinished[a]);
					  Object jsonOb = jj.getJSONObject("StationName").get("Zh_tw"); //抓取車站資料(測試用)
					  temp = jsonOb.toString();
					  if(a!=0 && !temp.equals(temp_2))station++; //過濾同車站多筆資料
					  temp_2 = jsonOb.toString();

					  Object direction = jj.get("Direction"); //抓取順逆行資料
					  if(direction.toString().equals("0"))along_tmp++;
					  else inverse_tmp++;

				  }
				  String[] station_name = new String[station]; //建立存取當前所有出現在動態上的車站數陣列
				  String[] along = new String[along_tmp];	   //建立順行總筆數陣列
				  String[] inverse = new String[inverse_tmp];  //建立逆行總筆數陣列
				  temp = "" ; 
				  temp_2 = ""; 
				  along_tmp = 0; 
				  inverse_tmp = 0; 
				  for(int a = 0; a < j.length() ; a++){
					  jj = new JSONObject(data_Unfinished[a]);
					  Object jsonob = jj.getJSONObject("StationName").get("Zh_tw");
					  
					  temp = jsonob.toString();
					  if(a != 0 && !temp.equals(temp_2)){
						  station_name[station_tmp] = temp_2;
						  station_tmp++;
					  } 
					  temp_2 = jsonob.toString();

					  Object direcTion = jj.get("Direction");
					  if(direcTion.toString().equals("0")){
						  along[along_tmp] = data_Unfinished[a];
						  along_tmp++;
					  }else {
						  inverse[inverse_tmp] = data_Unfinished[a];
						  inverse_tmp++;
					  }
				  }
				  station_name[station-1] = temp_2;
				  for(int a = 0;a < station;a++)System.out.print(station_name[a]+a+" ");
				  while(inverse_tmp >= 0 || along_tmp >= 0 ){
					  if(along_tmp >=0){
						  System.out.println(along[along_tmp-1]);
						  along_tmp--;
					  }else {
						  System.out.println(inverse[inverse_tmp]);
						  inverse_tmp--;
					  }
				  }
				  
				  System.out.println(station);
			  } catch(Exception e) {
				  System.out.println("error: "+e.getMessage());
				  e.printStackTrace();
			  }
		   }catch(ProtocolException e){
			   e.printStackTrace();
		   }  
		   catch(Exception e){
			   e.printStackTrace();
		   }
		 
		  
	}
	
    
	public static String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}

}
