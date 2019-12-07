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
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is the server Class
 * @author michael.hoff & Ramsey Kerley
 */
public class Server {

    String FileDirectory = "src/serverdh/documents/";

    //initialize socket and input stream 
    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private OutputStream out = null;
    private InputStream in = null;
    private int port;

    private byte[] clientPubKeyEnc;
    private Cipher bobCipher = null;

    // constructor with port 
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        this.port = port;
        while (true) {
            socket = null;
            try{
            System.out.println("Waiting");
            
            socket = serverSocket.accept();

            System.out.println("Connected to Client");

            out = socket.getOutputStream();
            in = socket.getInputStream();

            Thread client = new ClientHandler(serverSocket,out, in);
            client.start();
            }catch(Exception e){
                System.out.println(e.getCause());
            }
        }
    }
}
