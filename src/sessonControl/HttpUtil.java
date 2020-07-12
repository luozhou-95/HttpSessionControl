package sessonControl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;


public class HttpUtil {
	//timeout config
	private static volatile int socketTimeout = 120000;
	 
    private static volatile int connectTimeout = 30000;
    
    private static volatile int connectRequest = 30000;
    
    //max Connection
    private static volatile int maxRoute = 100;
    
    private static volatile int maxConnect = 100;
    
    //network params
    private static RequestConfig requestConfig;
    
    private static CloseableHttpClient httpClient;
    
    private static PoolingHttpClientConnectionManager cm;
    
    //set logger
    private static Logger logger =  Logger.getLogger(HttpUtil.class);
    
    public HttpUtil(){
    	init();
    }
    
    public HttpUtil(int maxRoute, int maxConnect){
    	HttpUtil.maxRoute = maxRoute;
    	HttpUtil.maxConnect = maxConnect;
    	init();
    }
    
    public HttpUtil(int maxRoute, int maxConnect, int socketTimeout, int connectTimeout, int connectRequest){
    	HttpUtil.maxRoute = maxRoute;
    	HttpUtil.maxConnect = maxConnect;
    	HttpUtil.socketTimeout = socketTimeout;
    	HttpUtil.connectTimeout = connectTimeout;
    	HttpUtil.connectRequest = connectRequest;
    	init();
    }

    private void init(){

    	//set timeout 
    	requestConfig = RequestConfig.custom()
    			.setConnectionRequestTimeout(connectRequest)
    			.setConnectTimeout(connectTimeout)
    			.setSocketTimeout(socketTimeout).build();
    	
    	//set PoolingHttpClientConnectionManager for multi threads
    	cm = new PoolingHttpClientConnectionManager();
    	cm.setDefaultMaxPerRoute(maxRoute);
    	cm.setMaxTotal(maxConnect);
    	//set session keep-alive time, default is 300 seconds 
    	ConnectionKeepAliveStrategy kaStrategy = new DefaultConnectionKeepAliveStrategy(){

			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				long keepAlive = super.getKeepAliveDuration(response, context);
				if (keepAlive == -1) {
					keepAlive = 300000;
				}
				return keepAlive;
			}
    		
    	};
    	//build client with StandardHttpRequestRetryHandler and PoolingHttpClientConnectionManager
    	httpClient = HttpClients.custom()
    			.setRetryHandler(new StandardHttpRequestRetryHandler())
    			.setKeepAliveStrategy(kaStrategy)
    			.setConnectionManager(cm).build();
    	//start monitor thread
    	new Thread(new IdleConnectionMonitorThread(cm)).start();
    }
    
    public void shutDownAllConnections(){
    	cm.shutdown();
    }
    
    public String post(String url, String xmlFileName){
    	//set httppost url
    	HttpPost hp = new HttpPost(url);
    	
    	//read xml entity 
    	BasicHttpEntity he = new BasicHttpEntity();
    	try {
    		FileInputStream fileInput = new FileInputStream(xmlFileName);
			he.setContent(fileInput);
			he.setContentLength(fileInput.available());
		} catch (FileNotFoundException e) {	
			e.printStackTrace();
			return null;
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    	
    	//set httppost config
    	hp.setConfig(requestConfig);
    	
    	//set httppost header
    	hp.setHeader("Content-type", "text/xml; charset=GBK");
    	
    	//set httppost entity
    	hp.setEntity(he);
    	try {
    		//try to get response
			CloseableHttpResponse response = httpClient.execute(hp);
			HttpEntity responseEntity = response.getEntity();
			StringBuffer sb = new StringBuffer();
			sb.append("send time: " + new Date().toGMTString() + '\n');
			sb.append("send to " + url + "\n");
			sb.append("response status: " + response.getStatusLine());
            if (responseEntity != null) {
            	sb.append("response content length: " + responseEntity.getContentLength() + "\n");
                String returnstr = EntityUtils.toString(responseEntity);
                sb.append("response: \n" + returnstr + '\n');
                logger.info(sb.toString());
                //return response here
                
                return returnstr;
            }
        //handle exception
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	//return null if there's exception
    	return null;
    }
    
    public String get(String url, String req){
    	//get method used to test 
    	long startTime = System.currentTimeMillis();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(requestConfig);
        CloseableHttpResponse response = null;
        try {
        	//get response from server
            response = httpClient.execute(httpGet);
            //get response entity
            HttpEntity responseEntity = response.getEntity();
            System.out.println("response status: " + response.getStatusLine());
            if (responseEntity != null) {
                System.out.println("response content length: " + responseEntity.getContentLength());
                String returnstr = EntityUtils.toString(responseEntity);
                System.out.println("response: \n" + returnstr);
                System.out.println("response time£º" + (System.currentTimeMillis() - startTime));
                return returnstr;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //close all the resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public static class IdleConnectionMonitorThread extends Thread{
    	private final HttpClientConnectionManager cm;
        private volatile boolean shutdown;
        
        public IdleConnectionMonitorThread(HttpClientConnectionManager cm){
        	this.cm = cm;
        }
        
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			while(!shutdown){
				
				try{
					synchronized(this){
						//close expired and idle connection every 60 seconds
						wait(60000);
						cm.closeExpiredConnections();
						cm.closeIdleConnections(60000, TimeUnit.MILLISECONDS);
					}
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
		
		public void shutdown(){
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
		}
		
		
        
    }
    
}
