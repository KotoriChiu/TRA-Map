import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.IllegalFormatWidthException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.*;

// javac -encoding utf-8 SignatureTest.java && java -Dfile.encoding=utf-8 SignatureTest

public class SignatureTest {
	static String[] data_Unfinished;
	static JSONArray j;
	static JSONObject jj;
	static JSONArray station_data;
	static String line, response="";
	static String[][][][] Train_time = new String[2][8][172][100];
	static String[][] master_station_name = new String[8][172];
	static String[] Station_txt = {"./Station_Data/南北回主線","./Station_Data/宜蘭支線","./Station_Data/山線","./Station_Data/成追線","./Station_Data/沙崙支線","./Station_Data/海線","./Station_Data/深澳平溪線","./Station_Data/縱貫線"};
	static String[] Station_route = {"南北回主線","宜蘭支線","山線","成追線","沙崙支線","海線","深澳平溪線","縱貫線"};
	static int[] Station_numbers = {0,0,0,0,0,0,0,0};
	static String[] along;
	static String[] inverse;
	static int along_tmp , inverse_tmp; 
	public static void main(String[] args) {
				long startTime = System.currentTimeMillis();
			  	start_readAPI(); 						//取得API資料
			  	try {
				  	start_arrange_dada();				//處理抓到的API資料
				  	data_detail_process();				//依照順行逆行將資料分開整理
					liveboard_data();					//讀取本地資中的車站資訊
					Straight_retrograde_processing();	//將先前整理好的順逆行資料 分別存在對應的車站陣列中
					System.out.println("資料抓取與處理共花了:" + (System.currentTimeMillis() - startTime)/1000.0 + "秒");
			  	} catch(Exception e) {
				  	System.out.println("error: "+e.getMessage());
				  	e.printStackTrace();
			  	}  		  
	}

	public static void start_readAPI() {
		HttpURLConnection connection = null;
		String[] APP_Url_Id_Key = new String[3];      
		String[] txtdata = {"http://ptx.transportdata.tw/MOTC/v2/Rail/TRA/LiveBoard?$format=JSON","XXXXXXXXXXXXXXXXX  APPID","XXXXXXXXXXXXXXXXX  APPKey"};
        System.setProperty("file.encoding", "UTF-8");
        String xdate = getServerTime();
        String SignDate = "x-date: " + xdate;        
		String Signature="";
		File file = new File("API_key");
		try {
			if(!file.exists()){ //判斷密鑰檔是否存在
				FileWriter fw = new FileWriter("API_key");
				for(int i = 0;i < 3;i++)fw.write(txtdata[i]+"\n"); //寫入預設密鑰檔內容
				fw.flush();
				fw.close();
				System.out.println("文件檔\"API_key\"已新增,請至目錄下設定AppID AppKey!!!");
				System.exit(0); //當密鑰文件檔未創立 自動創立完 且停止程式執行
			}
			FileReader fr = new FileReader("API_key");
			BufferedReader API_key = new BufferedReader(fr); //更新寫法 用文字檔讀取Url ID Key
			for(int i = 0;i<APP_Url_Id_Key.length;i++) APP_Url_Id_Key[i] = API_key.readLine();
			fr.close();
			if(!(APP_Url_Id_Key[1].matches("[a-z0-9]{32}") && APP_Url_Id_Key[2].matches("\\w{27}"))){
				System.out.println("您輸入的密鑰格式似乎不太正確 請確認您的密鑰!!");
				System.exit(1);
			}//71a1de694a034562a7356e8dce5c5791 
			Signature = HMAC_SHA1.Signature(SignDate, APP_Url_Id_Key[2]);
		} catch (SignatureException e1) {
			e1.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		//System.out.println("Signature :" + Signature);
        String sAuth = "hmac username=\"" + APP_Url_Id_Key[1] + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"" + Signature + "\"";
        //System.out.println(sAuth);
		   try{  
		      URL url=new URL(APP_Url_Id_Key[0]);
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
			}catch(ProtocolException e) {
				e.printStackTrace();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
	}

	public static void start_arrange_dada() {
		try{
			j = new JSONArray(response);
			data_Unfinished = new String[j.length()];
			FileWriter fw = new FileWriter("output");
			
			for (int a=0; a < j.length(); a++) { //整理JSON
				data_Unfinished[a] = String.valueOf(j.get(a));
		    	System.out.println(data_Unfinished[a] + "\n");
				fw.write(data_Unfinished[a] + "\n");
			}
				fw.flush(); 
				fw.close();
				System.out.println("共抓到" + j.length() + "筆資料");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void data_detail_process() {
		try{
			String temp = "", temp_2 = "";
			int station = 1, station_tmp = 0;
			along_tmp = 0;   //順行計次
			inverse_tmp = 0; //逆行計次
			for(int a = 0; a < j.length() ; a++) {
				jj = new JSONObject(data_Unfinished[a]); 
				Object jsonOb = jj.getJSONObject("StationName").get("Zh_tw"); //抓取車站資料(測試用)
				temp = jsonOb.toString();
				if(a != 0 && !temp.equals(temp_2)) station++; //計算過濾同車站多筆資料
				temp_2 = jsonOb.toString();

				Object direction = jj.get("Direction"); //抓取順逆行資料
				if(direction.toString().equals("0")) along_tmp++;
				else inverse_tmp++;
			}	
			String[] station_name = new String[station]; //建立存取當前所有出現在動態上的車站數陣列
			along = new String[along_tmp];	   //建立順行總筆數陣列
			inverse = new String[inverse_tmp];  //建立逆行總筆數陣列
			temp = "";
			temp_2 = "";
			along_tmp = 0;
			inverse_tmp = 0; 

			for(int a = 0; a < j.length(); a++) {
				jj = new JSONObject(data_Unfinished[a]);
				Object jsonob = jj.getJSONObject("StationName").get("Zh_tw"); 
				temp = jsonob.toString();
				if(a != 0 && !temp.equals(temp_2)) { //多筆同樣車站名的資料過濾成一個 存進陣列
					station_name[station_tmp] = temp_2;
					station_tmp++;
				} 
				temp_2 = jsonob.toString();
				Object direcTion = jj.get("Direction"); //抓取資料的"Direction"參數(順逆行參數)
				if(direcTion.toString().equals("0")) {  //辨別資料是順行還逆行 分別存在順逆行的陣列
					along[along_tmp] = data_Unfinished[a];
					along_tmp++;
				}else {
					inverse[inverse_tmp] = data_Unfinished[a];
					inverse_tmp++;
				}
			}
			station_name[station - 1] = temp_2;
			for(int a = 0; a < station; a++) System.out.print(station_name[a] + a + " ");
			System.out.println("順行資料共有:" + along_tmp + "筆 逆行資料共有:" + inverse_tmp + "筆");
		}catch(Exception e) {
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
			for(int i = 0;i < master_station_name.length;i++){
				for(int j = 0;j<master_station_name[0].length;j++)if(master_station_name[i][j] != null)Station_numbers[i]++;
				System.out.println(Station_numbers[i]);
				Train_time[0][i] =new String[Station_numbers[i]][100]; 
				Train_time[1][i] =new String[Station_numbers[i]][100]; 
			}
			for(String[] i : master_station_name){
				for(String j : i){
					if(j != null)System.out.print(j+" ");
					else break;
				}
				System.out.println();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void Straight_retrograde_processing(){
		//data_Unfinished[] api抓到的資料 
		//along[]順行
		//inverse[]逆行
		//Train_time[2][8][每條線的車站數量][] 準備存入API資料的四維陣列  [2]順逆行[8]線數[X]車站量[X]該車站的API資料
		//int Station_numbers[]記錄每條線的車站數量
		//string master_station_name[8][] 紀錄每條線的車站名 拿來比對資料是否為該站的資料
		try{
			int Data_along_temp = 0;
			int Data_inverse_temp = 0;
			for(int i=0;i<8;i++){
				for(int j=0;j<Station_numbers[i];j++){
					for(int API=0;API<along.length;API++){
						JSONObject A_along = new JSONObject(along[API]);
						Object API_DATA = A_along.getJSONObject("StationName").get("Zh_tw");
						String Api_Data = API_DATA.toString();
						if(master_station_name[i][j].equals(Api_Data))Data_along_temp++;
					}
					if(Data_along_temp!=0)Train_time[0][i][j] = new String[Data_along_temp];
					Data_along_temp = 0;
					for(int API=0;API<along.length;API++){
						JSONObject A_along = new JSONObject(along[API]);
						Object API_DATA = A_along.getJSONObject("StationName").get("Zh_tw");
						String Api_Data = API_DATA.toString();
						if(master_station_name[i][j].equals(Api_Data)){
							Train_time[0][i][j][Data_along_temp] = along[API];
							Data_along_temp++;
						}
					}
					Data_along_temp = 0;
					for(int API=0;API<inverse.length;API++){
						JSONObject A_inverse = new JSONObject(inverse[API]);
						Object API_DATA = A_inverse.getJSONObject("StationName").get("Zh_tw");
						String Api_Data = API_DATA.toString();
						if(master_station_name[i][j].equals(Api_Data))Data_inverse_temp++;
					}
					if(Data_inverse_temp!=0)Train_time[1][i][j] = new String[Data_inverse_temp];
					Data_inverse_temp = 0;
					for(int API=0;API<inverse.length;API++){
						JSONObject A_inverse = new JSONObject(inverse[API]);
						Object API_DATA = A_inverse.getJSONObject("StationName").get("Zh_tw");
						String Api_Data = API_DATA.toString();
						if(master_station_name[i][j].equals(Api_Data)){
							Train_time[1][i][j][Data_inverse_temp] = inverse[API];
							Data_inverse_temp++;
						}
					}
					Data_inverse_temp = 0;
				}	
			}
			//輸出格式 XX站 車次 車種 到站時間 駛離時間 終點: 準點與否:
			File dataTXT = new File("dataTXT.txt");
			dataTXT.createNewFile();
			FileWriter fw = new FileWriter("dataTXT.txt");
			int ii = 0;
			int oo = 0;
			for(String a[][][] : Train_time){
				fw.write("順行逆行分隔\n");
				for(String b[][] : a){
					fw.write("路線分隔\n");
					for(String c[] : b){
						for(String d : c){
							if(d!=null){
								System.out.println(d);
								JSONObject write_txt = new JSONObject(d);
								Object[] Station_inif = new Object[7];
								String[] Station_inif_Str = new String[7];
								Station_inif[0] = write_txt.getJSONObject("StationName").get("Zh_tw");
								Station_inif[1] = write_txt.get("StationID");
								Station_inif[2] = write_txt.getJSONObject("TrainTypeName").get("Zh_tw");
								Station_inif[3] = write_txt.get("ScheduledArrivalTime");
								Station_inif[4] = write_txt.get("ScheduledDepartureTime");
								Station_inif[5] = write_txt.getJSONObject("EndingStationName").get("Zh_tw");
								Station_inif[6] = write_txt.get("DelayTime");
								for(int i = 0;i<Station_inif.length;i++)Station_inif_Str[i] = Station_inif[i].toString();
								fw.write("當前車站名稱:"+Station_inif_Str[0]+"站 車次:"+Station_inif_Str[1]+"次 車種:"+Station_inif_Str[2]+" 預定到站時間:"+Station_inif_Str[3]+" 預定駛離時間:"+Station_inif_Str[4]+" 本列車終點:"+Station_inif_Str[5]+"站 誤點與否:"+(Station_inif_Str[6].equals("0")?"準點":("誤點"+Station_inif_Str[6]+"分"))+"\n");
							}
						}
					}
				}
			}
			fw.flush();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}
}