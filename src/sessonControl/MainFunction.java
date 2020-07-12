package sessonControl;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.log4j.Logger;

public class MainFunction {

	public static void main(String[] args) {
		
		HttpUtil hu= new HttpUtil();
		for(int i = 1; i < 10; i++){
			new Thread(new Client(hu)).start();
		}	
	}

	

}
