package se.payerl.packetbitratecalculator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.packet.Packet;

public class Start {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Start calc = new Start();
		double mean = calc.calculateMean();
		double standardDeviation = calc.calculateStandardDeviation(mean);
		System.out.println(String.format("Mean: %.3f Bps. (%.3f Mbps)", mean, (mean*8/1024/1024)));
		System.out.println(String.format("Standard deviation: %.3f Bps. (%.3f Mbps)", standardDeviation, (standardDeviation * 8 / 1024 / 1024)));
	}
	
	private List<Packet> packetList;
	
	/**
	 * Loads .pcap file into List of packets. Throws any exceptions.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Start() throws FileNotFoundException, IOException {
		packetList = new ArrayList<>();
		
		JFileChooser jfc = new JFileChooser();
		jfc.showOpenDialog(null);
		
		final Pcap pcap = Pcap.openStream(jfc.getSelectedFile());
		pcap.loop(new PacketHandler() {
			@Override
			public void nextPacket(Packet packet) throws IOException {
				packetList.add(packet);
			}
		});

		System.out.println("Number of packets in '" + jfc.getSelectedFile().getName() + "': " + packetList.size());
	}
	
	/**
	 * Returns bps rate based on the current packet.
	 * @param current The packet to base the calculation on.
	 * @param previous The packet before (to be able to know when transmission started.
	 * @return double of bytes per second.
	 */
	private double getBytesPerSecond(Packet current, Packet previous) {
		if(previous != null && current != null) {
			final int MICROSECONDS_IN_SECOND = 1000000;
			double sizeInBytes = (double) current.getPayload().getReadableBytes();
			double durationInMicroseconds = ((double) current.getArrivalTime()) - ((double) previous.getArrivalTime());
			return sizeInBytes / (durationInMicroseconds/MICROSECONDS_IN_SECOND);
		} else {
			throw new NullPointerException("A packet was null.");
		}
	}
	
	/**
	 * Calculates the mean of all packets in the file.
	 * @return double value of the mean.
	 * @throws IOException
	 */
	public double calculateMean() throws IOException {
		double collectionBps = 0;
		int count = 0;
		
		for(int index = 1; index < packetList.size(); index++) {
			Packet previous = packetList.get(index-1);
			Packet current = packetList.get(index);
			
			if((((double) current.getArrivalTime()) - ((double) previous.getArrivalTime())) > 10) {
				collectionBps += getBytesPerSecond(current, previous);
				count++;
			}
		}
		return (collectionBps / count);
	}
	
	/**
	 * Calculates the standard deviation from the mean.
	 * @param mean The mean value retrieved from .calculateMean() function.
	 * @return double value of standard deviation.
	 */
	public double calculateStandardDeviation(double mean) {
		double collectionBps = 0;
		int count = 0;
		
		for(int index = 1; index < packetList.size(); index++) {
			Packet previous = packetList.get(index-1);
			Packet current = packetList.get(index);
			
			if((((double) current.getArrivalTime()) - ((double) previous.getArrivalTime())) > 10) {
				collectionBps += Math.pow(getBytesPerSecond(current, previous) - mean, 2);
				count++;
			}
		}
		return Math.sqrt(collectionBps / count);
	}
}