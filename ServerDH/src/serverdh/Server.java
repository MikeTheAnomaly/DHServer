/*
 * Server example used from "https://www.geeksforgeeks.org/socket-programming-in-java/" 
 * by Souradeep Barua
 */
package serverdh;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;

/**
 *
 * @author michael.hoff
 */
public class Server {

    //initialize socket and input stream 
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;

    // constructor with port 
    public Server(int port) throws IOException{
        // starts server and waits for a connection 
            server = new ServerSocket(port);
            System.out.println("Server started on port: " + port);

            System.out.println("Waiting for a client ...");
            
            socket = server.accept();
            System.out.println("Client accepted" + socket.getRemoteSocketAddress().toString());
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            System.out.println(socket.getInputStream().toString());
            // takes input from the client socket 
            in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            String line = "";

            // reads message from client until "Over" is sent 
            while (!line.equals("Over")) {
                try {
                    line = in.readUTF();
                    System.out.println(line);

                } catch (IOException i) {
                    System.out.println(i);
                }
            }
            System.out.println("Closing connection");

            // close connection 
            socket.close();
            in.close();
    }
}
