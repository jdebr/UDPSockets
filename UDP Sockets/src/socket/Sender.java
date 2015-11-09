package socket;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Sender implements Runnable{
	
	public static void printWindow(int low, int high){
		if(low < 0){
			System.out.println("[- - - -]");
		} else if(high == -1){
			System.out.println("[" + low + " " + (low+1) + " " + (low+2) + " -]");
		} else if(high == -2){
			System.out.println("[" + low + " " + (low+1) + " - -]");
		} else if(high == -3){
			System.out.println("[" + low + " - - -]");
		} else {
			System.out.println("[" + low + " " + (low+1) + " " + (low+2) + " " + (low+3) + "]");
		}
	}
	
	//public static void main(String[] args){
	public void run(){
		try{
			// Get initial running info from user
			Scanner reader = new Scanner(System.in);
			boolean sizeCheck = true;
			int windowSize = 0;
			int maxSequence = 0;
			while(sizeCheck){
				System.out.println("Enter window size:");
				windowSize = reader.nextInt();
				System.out.println("Enter max sequence number:");
				maxSequence = reader.nextInt();
				if((windowSize/maxSequence)>(1/2)){
					System.out.println("Window size must be half of Max Sequence or less");
				} else {sizeCheck = false;}
			}
			System.out.println("Select packet to drop:");
			int droppedPacket = reader.nextInt();
			reader.close();
			int windowLower = 0;
			int windowUpper = windowSize - 1;
			Timer[] timers = new Timer[maxSequence];
			
			// Open new UDP socket
			DatagramSocket senderSocket = new DatagramSocket(9877);
			
			// Get receiver IP
			//InetAddress IPAddress = InetAddress.getByName("174.45.91.222");
			InetAddress IPAddress = InetAddress.getLocalHost();
			
			// Initialize arrays for UDP packets
			byte[] sendData = new byte[4];
			byte[] rcvData = new byte[4];
			
			// Prepare Window Size and Maximum Sequence # for sending
			sendData[0] = (byte)windowSize;
			sendData[1] = (byte)maxSequence;
			
			// Build packets
			DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
			DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
			
			// Send Window Size and Max Seq #
			senderSocket.send(sendPkt);
			System.out.println("Sending window size and max sequence number to receiver.");
			
			// Wait for ACK
			senderSocket.receive(rcvPkt);
			int w = rcvData[0];
			int m = rcvData[1];
			
			// Begin data transfer if proper ACK received
			int currentAck = -1;
			int ackBuffer = -1;
			if(windowSize == w && maxSequence == m){
				System.out.println("Confirmation received, ready to send");
				// Loop through entire sequence
				for(int i = 0; i < maxSequence; i+=0){
					// Loop through current window
					int sentPackets = 0;
					for(int j = i; j <= windowUpper; j++){
						// Send packets in window (simulate dropped packet by not sending)
						if(j!=droppedPacket){
							sendData[0] = (byte)j;
							sendPkt = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
							senderSocket.send(sendPkt);
							Thread.sleep(500);
						}
						// Start timer for sent packet to receive ACK
						timers[j] = new Timer();
						timers[j].schedule(new TimerTask() {
							private int myPkt;
							@Override
							public void run(){
								System.out.println("Packet " + myPkt + " times out.  Resending Packet " + myPkt);
								byte[] timerData = new byte[4];
								timerData[0] = (byte)myPkt;
								DatagramPacket timerPkt = new DatagramPacket(timerData, timerData.length, IPAddress, 9876);
								try {
									senderSocket.send(timerPkt);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							private TimerTask init(int pkt){
								myPkt = pkt;
								return this;
							}
							
						}.init(j), 5*1000);
						System.out.print("Sent packet " + j + ". Window: ");
						printWindow(windowLower, windowUpper);
						sentPackets++;
					}
					// After sending all possible packets, wait for an ACK
					senderSocket.receive(rcvPkt);
					currentAck = rcvData[0];
					timers[currentAck].cancel();
					// If ACK is in order, increment window
					if(currentAck == windowLower){
						// If you have buffered ACKS, set window accordingly
						if(ackBuffer>0){
							windowLower = ackBuffer + 1;
							windowUpper = ackBuffer + windowSize;
							ackBuffer = -1;
						} else {
							windowLower++;
							if(windowUpper < 0){
								windowUpper --;
							} else{
								windowUpper++;
							}
						}
						if((windowUpper > (maxSequence-1))){
							windowUpper = -1;
						}
					}else{
						// If not, remember which ACKs have been received
						ackBuffer = currentAck;
					}
					System.out.print("Ack" + currentAck + " received. Window: ");
					printWindow(windowLower, windowUpper);
					i += sentPackets;
				}
			}
			// Finish waiting for ACKS after all packets sent
			while(true){
				senderSocket.receive(rcvPkt);
				currentAck = rcvData[0];
				timers[currentAck].cancel();
				// If ACK is in order, increment window
				if(currentAck == windowLower){
					// If you have buffered ACKS, set window accordingly
					if(ackBuffer>0){
						windowLower = ackBuffer + 1;
						windowUpper = ackBuffer + windowSize;
						ackBuffer = -1;
					} else {
						windowLower++;
						if(windowUpper < 0){
							windowUpper --;
						} else{
							windowUpper++;
						}
					}
					if((windowUpper > (maxSequence-1))){
						windowUpper = -1;
					}
					// End of Sequence Reached.
					if(windowLower > (maxSequence-1)){
						System.out.print("Ack" + currentAck + " received. Window: ");
						printWindow(-1, windowUpper);
						senderSocket.close();
						System.exit(0);
					}
				}
				System.out.print("Ack" + currentAck + " received. Window: ");
				printWindow(windowLower, windowUpper);
			}
		}
		catch(Exception e){
			System.out.println("Exception occurred in sender");
		}
	}

}
