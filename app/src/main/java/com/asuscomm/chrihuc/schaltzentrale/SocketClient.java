package com.asuscomm.chrihuc.schaltzentrale;
import java.io.*;
import java.net.*;

public class SocketClient  {
    public static void main(String args) throws IOException {
        DatagramSocket socket = new DatagramSocket();

        byte[] buf = new byte[1024];
        buf = args.getBytes();
        InetAddress address = InetAddress.getByName("192.168.192.10"); //InetAddress.getLocalHost();
        //int test = Integer.parseInt(args);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 5000);

        //System.out.println("Sending...");
        socket.send(packet);

        //System.out.println("Receiving...");
        //packet = new DatagramPacket(buf, buf.length);
        //socket.receive(packet);

        //String received = new String(packet.getData(), 0, packet.getLength());
        //System.out.println(received);
        //System.out.println("Done!");

        socket.close();
    }
}

