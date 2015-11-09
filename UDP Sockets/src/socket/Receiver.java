package socket;

import java.io.*;
import java.net.*;

public class Receiver implements Runnable{

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
		DatagramSocket receiverSocket = null;
		try{
			// Open UDP Socket
			receiverSocket = new DatagramSocket(9876);
			
			// Initialize byte arrays for UDP packets 
			byte[] rcvData = new byte[4];
			byte[] sendData = new byte[4];
			
			// Listen for initial message of Window Size and Max Seq #
			DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
			receiverSocket.receive(rcvPkt);
			
			// Set variables and get return IP and port #
			int windowSize = rcvData[0];
			int maxSequence = rcvData[1];
			int windowLower = 0;
			int windowUpper = windowSize-1;
			InetAddress returnAddress = rcvPkt.getAddress();
			int returnPort = rcvPkt.getPort();			
			
			// ACK by returning the received packet
			sendData[0] = (byte)windowSize;
			sendData[1] = (byte)maxSequence;
			DatagramPacket ackPkt = new DatagramPacket(sendData, sendData.length, returnAddress, returnPort);
			receiverSocket.send(ackPkt);
			
			// Main transmission loop
			int currentSeq = -1;
			int pktBuffer = -1;
			while(true){
				receiverSocket.receive(rcvPkt);
				currentSeq = rcvData[0];
				// If packet is in order, increment window
				if(currentSeq == windowLower){
					if(pktBuffer > 0){
						windowLower = pktBuffer + 1;
						windowUpper = pktBuffer + windowSize;
						pktBuffer = -1;
					} else {
						if(windowLower < (maxSequence-1)){
							windowLower++;
						} else {
							windowLower = -1;
						}
						if(windowUpper < 0){
							windowUpper --;
						} else{
							windowUpper++;
						}
					}
					if((windowUpper > (maxSequence-1))){
						windowUpper = -1;
					}
				} else {
					// If packets not in order, remember which packets are received
					pktBuffer = currentSeq;
				}
				sendData[0] = (byte)currentSeq;
				ackPkt = new DatagramPacket(sendData, sendData.length, returnAddress, returnPort);
				receiverSocket.send(ackPkt);
				System.out.print("Packet " + currentSeq + " received.  Ack" + currentSeq + " sent. Window: ");
				printWindow(windowLower, windowUpper);
			}
		} catch(SocketException e){
			System.out.println("SocketException occurred in receiver");
		} catch(IOException e){
			System.out.println("IOException occurred in receiver");
		} finally {
			if(receiverSocket != null){
				receiverSocket.close();
			}
		}
	}
}
