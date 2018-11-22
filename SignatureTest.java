import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
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
import org.json.*;

//java -Dfile.encoding=utf-8  SignatureTest
//javac -encoding utf-8 SignatureTest.java


public class SignatureTest {
	static String[] data_Unfinished;
	static JSONArray j;
	static JSONArray station_data;
	static String line, response="";
	static String[][][][] Train_time = new String[2][8][][];
	static String[][] master_station_name = new String[8][172];
	public static void main(String[] args) {
				long startTime = System.currentTimeMillis();
			  	start_readAPI(); //取得API資料
			  	//System.out.println(response);
			  	try {
				  	start_arrange_dada();
				  	data_detail_process();
					liveboard_data();
					System.out.println("Using Time:" + (System.currentTimeMillis() - startTime) + " ms");
			  	} catch(Exception e) {
				  	System.out.println("error: "+e.getMessage());
				  	e.printStackTrace();
			  	}  		  
	}
	
	public static void start_readAPI(){
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
	          while((bytesRead = inputStream.read(buff)) != -1) bao.write(buff, 0, bytesRead);
	          
	          
		      ByteArrayInputStream bais = new ByteArrayInputStream(bao.toByteArray());
		      GZIPInputStream gzis = new GZIPInputStream(bais);
		      InputStreamReader reader = new InputStreamReader(gzis);
		      BufferedReader in = new BufferedReader(reader);
		      while ((line = in.readLine()) != null) response+=(line+"\n");
		      
		      Type RailStationListType = new TypeToken<ArrayList<RailStation>>(){}.getType();
		      Gson gsonReceiver = new Gson();
		      List<RailStation> obj = gsonReceiver.fromJson(response, RailStationListType);
			}catch(ProtocolException e){
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
	}
	
	public static void start_arrange_dada(){
		File file = new File("output.txt");
		try{
			j = new JSONArray(response);
			data_Unfinished = new String[j.length()];
			FileWriter fw = new FileWriter(file);
			
			for (int a=0; a < j.length(); a++) { //整理JSON
				data_Unfinished[a] = String.valueOf(j.get(a));
		    	System.out.println(data_Unfinished[a] + "\n");
				fw.write(data_Unfinished[a] + "\n");
			}
				fw.flush(); 
				fw.close();
				System.out.println("共抓到"+j.length()+"筆資料");
		}catch(Exception e){
			e.printStackTrace();	
		}
	}

	public static void data_detail_process() {
		JSONObject jj;
		try{
			String temp="",temp_2="";
			int station = 1,station_tmp = 0;
			int along_tmp = 0,inverse_tmp = 0; //順行計次,逆行計次
			for(int a = 0; a < j.length() ; a++){
				jj = new JSONObject(data_Unfinished[a]); 

				Object jsonOb = jj.getJSONObject("StationName").get("Zh_tw"); //抓取車站資料(測試用)
				temp = jsonOb.toString();
				if(a!=0 && !temp.equals(temp_2))station++; //計算過濾同車站多筆資料
				temp_2 = jsonOb.toString();

				Object direction = jj.get("Direction"); //抓取順逆行資料
				if(direction.toString().equals("0"))along_tmp++;
				else inverse_tmp++;
			}	
			String[] station_name = new String[station]; //建立存取當前所有出現在動態上的車站數陣列
			String[] along = new String[along_tmp];	   //建立順行總筆數陣列
			String[] inverse = new String[inverse_tmp];  //建立逆行總筆數陣列
			temp = "" ; temp_2 = ""; along_tmp = 0; inverse_tmp = 0; 

			for(int a = 0; a < j.length() ; a++){
				jj = new JSONObject(data_Unfinished[a]);
				Object jsonob = jj.getJSONObject("StationName").get("Zh_tw"); 
					  
				temp = jsonob.toString();
				if(a != 0 && !temp.equals(temp_2)){ //多筆同樣車站名的資料過濾成一個 存進陣列
					station_name[station_tmp] = temp_2;
					station_tmp++;
				} 
				temp_2 = jsonob.toString();

				Object direcTion = jj.get("Direction"); //抓取資料的"Direction"參數(順逆行參數)
				if(direcTion.toString().equals("0")){  //辨別資料是順行還逆行 分別存在順逆行的陣列
					along[along_tmp] = data_Unfinished[a];
					along_tmp++;
				}else {
					inverse[inverse_tmp] = data_Unfinished[a];
					inverse_tmp++;
				}
			}
			station_name[station-1] = temp_2;
			for(int a = 0;a < station;a++) System.out.print(station_name[a]+a+" ");
			System.out.println("順行資料共有:"+along_tmp+"筆 逆行資料共有:"+inverse_tmp+"筆");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void liveboard_data(){
		
		JSONObject[] Station_DataJSON = new JSONObject[8];
		Object[] jsonSD = new Object[8];
		JSONArray[] SDA = new JSONArray[8];
		String[][] SDS = new String[8][];
		String[] master = {"","","","","","","",""};
		try{
			FileReader[] file = new FileReader[8];
			String[] Station_txt = {"./Station_Data/南北回主線.txt","./Station_Data/宜蘭支線.txt","./Station_Data/山線.txt","./Station_Data/成追線.txt","./Station_Data/沙崙支線.txt","./Station_Data/海線.txt","./Station_Data/深澳平溪線.txt","./Station_Data/縱貫線.txt"};
			for(int a = 0;a<8;a++)file[a] = new FileReader(Station_txt[a]);
			BufferedReader[] br= new BufferedReader[8];
			for(int a = 0;a<8;a++){
				br[a] =new BufferedReader(file[a]);
				while(br[a].ready())master[a]+=br[a].readLine();
				file[a].close();
				SDA[a] = new JSONArray(master[a]);
				SDS[a] = new String[SDA[a].length()];
				for(int i = 0; i<SDA[a].length();i++){
					SDS[a][i] = String.valueOf(SDA[a].get(i));
					Station_DataJSON[a] = new JSONObject(SDS[a][i]);
					jsonSD[a] = Station_DataJSON[a].getJSONObject("StationName").get("Zh_tw");
					master_station_name[a][i] = jsonSD[a].toString();
				}
			}
			int[] Station_numbers = {0,0,0,0,0,0,0,0};
			for(int i = 0;i < master_station_name.length;i++){
				for(int j = 0;j<master_station_name[0].length;j++)if(master_station_name[i][j] != null)Station_numbers[i]++;
				System.out.println(Station_numbers[i]);
				Train_time[0][i] =new String[Station_numbers[i]][]; 
				Train_time[1][i] =new String[Station_numbers[i]][]; 
			}
			for(String[] i : master_station_name){
				for(String j : i){
					if(j != null)System.out.print(j+" ");
					else break;
				}
				System.out.println();
			}
			//System.out.println(master_station_name[0][1]);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void Straight_retrograde_processing(){
		JSONObject data;
		try{
			//這邊要做將四維陣列Train_time[2][8][X][XX] 的XX放入API抓下來的資料 並且順逆行分開放在Train_time[0][8][X][XX]跟Train_time[1][8][X][XX]
			for(int a = 0;a<data_Unfinished.length;a++){
				data = new JSONObject(data_Unfinished[a]);
				Object jsonStation = jj.getJSONObject("StationName").get("Zh_tw");
				//Object json
			}
		}catch(Exception e){
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


