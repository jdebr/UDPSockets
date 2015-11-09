package socket;

import java.util.concurrent.TimeUnit;

public class Driver {

	public static void main(String[] args) {
		Thread sender = new Thread(new Sender() );
		Thread receiver = new Thread(new Receiver());
		
		receiver.start();
		try{
			TimeUnit.SECONDS.sleep(3);
		} catch(Exception e){
			System.out.println("Exception occurred in driver");
		}
		sender.start();

	}

}
