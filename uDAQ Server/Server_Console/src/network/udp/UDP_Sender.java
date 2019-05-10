package network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import gndm.network.center.command.Command;

public class UDP_Sender {
 
    public static void send(Command c, InetSocketAddress dest) throws IOException 
    {
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = c.toBuffer().array();
        
        DatagramPacket packet = new DatagramPacket(buf, buf.length, dest);
        socket.send(packet);
        socket.close();
        
        System.out.println("Multicast of length " + buf.length + " sent.");
        System.out.println("Message length = " + c.Header().message_length);
        System.out.println("Command ID = " + c.Header().command_id);
        System.out.println("Message = " + c.toString());
    }
}
