
import java.util.Calendar;
public class test{
    public static void main(String args []){
        String str[] = {""," I","was","duckonfucking!","onlyJAVA"};
        System.out.println(str[0].isEmpty());
        System.out.println(str[0].replace("", "Welcome,")+str[1].replace(" I", "I ")+str[2].replace("was", "am ")+str[3].replace(str[3], "doing ")+str[4].replace(str[4], "JAVA"));
        
        for(int i=0;i<str.length;i++){
            for(int j=0;j<i;j++){
                if(str[i].compareTo(str[j])>0){
                    String tmp = str[i];
                    str[i] = str[j];
                    str[j] = tmp;
                }
            }
        }
        for(int a = 0;a<str.length;a++){
            str[a] =str[a].toUpperCase();
           str[a]= str[a].trim();
        }
        for(int a = 0;a<str.length;a++){
            System.out.println("第"+(a+1)+"個字串為:"+str[a]);
        }

    }
}