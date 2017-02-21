package com.asuscomm.chrihuc.schaltzentrale;
import java.io.*;
import java.net.*;

public class SocketClient  {
    public static void broadc(String args) throws IOException {
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

    public static void request(String args) throws IOException {
        //DatagramSocket socket = new DatagramSocket();

        //byte[] buf = new byte[1024];
        //buf = args.getBytes();
        //InetAddress address = InetAddress.getByName("192.168.192.10"); //InetAddress.getLocalHost();
        //int test = Integer.parseInt(args);
        //DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 5005);

        //System.out.println("Sending...");
        //socket.send(packet);

        //System.out.println("Receiving...");
        //packet = new DatagramPacket(buf, buf.length);
        //socket.receive(packet);

        //String received = new String(packet.getData(), 0, packet.getLength());
        //System.out.println(received);
        //System.out.println("Done!");

        //socket.close();

        Socket s = new Socket("192.168.192.10", 5005);
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        //dos.writeUTF(args);
        dos.writeBytes(args);


        //read input stream
        DataInputStream dis2 = new DataInputStream(s.getInputStream());
        InputStreamReader disR2 = new InputStreamReader(dis2);
        BufferedReader br = new BufferedReader(disR2);//create a BufferReader object for input
        String line = br.readLine();
        String output = br.toString();
        System.out.println(output);
        dis2.close();
        s.close();

    }
}

