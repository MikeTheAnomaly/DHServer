/*
 * Server example used from "https://www.geeksforgeeks.org/socket-programming-in-java/" 
 * by Souradeep Barua
 */

//https://stackoverflow.com/questions/38271609/client-server-porgram-file-encrypt-at-server-and-decrypt-at-client-java
//https://www.geeksforgeeks.org/socket-programming-in-java/
//https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
//https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
package serverdh;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 *
 * @author michael.hoff
 */
public class Server {

    //initialize socket and input stream 
    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private BufferedReader br = null;

    // constructor with port 
    public Server(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        System.out.println("Connected to Client");
        in = socket.getInputStream();
        out = socket.getOutputStream();

        byte[] bytes = new byte[16 * 1024];  
        //Put in seprate socket for sending data where in is fileinputstream
        int count;
        while ((count = in.read(bytes)) > 0) {
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            System.out.println(result);
            out.write(bytes, 0, count);
        }

        out.close();
        in.close();
        socket.close();
        serverSocket.close();
    }
}
