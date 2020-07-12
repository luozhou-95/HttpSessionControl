package sessonControl;

import org.apache.log4j.Logger;


public class Client implements Runnable{
	
	private static volatile int ID = 1;
	private HttpUtil httpUtil;
	private static Logger logger =  Logger.getLogger(Client.class);
	
	public Client(HttpUtil httpUtil){
		this.httpUtil = httpUtil;
	}
	
	public void run() {
		int threadID;
		synchronized(this){
			threadID = ID;
			ID += 1;
		}
		while(true){
			try {
				Thread.currentThread().sleep(10000);
				httpUtil.post("http://localhost:8080","xmlTest.txt");
			} catch (InterruptedException e) {
				//if interrupted 
				logger.info("Thread " + Thread.currentThread().getName() + "has finished \n");
				break;
			}
		}
		
	}
	
	

}
