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
 *
 * @author michael.hoff
 */
public class Server {

    String FileDirectory = "src/serverdh/documents/";

    //initialize socket and input stream 
    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private OutputStream out = null;
    private InputStream in = null;

    private byte[] clientPubKeyEnc;
    private Cipher bobCipher = null;

    // constructor with port 
    public Server(int port) throws IOException {

        System.out.println("Waiting");
        serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        System.out.println("Connected to Client");

        out = socket.getOutputStream();
        in = socket.getInputStream();

        try {
            KeyCreation();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println();

        HandleFiles();

        out.close();
        socket.close();
        serverSocket.close();
    }

    private byte[] Read() {
        return null;
    }

    public void HandleFiles() throws IOException {
        File directory = new File(FileDirectory);
        System.out.println(getFileNames(directory));
        String filesList = getFileNames(directory);
        try {
            sendToClient(filesList.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        //This is not required but is good practce to allow the client to retry
        //Though currently client does not catch its own inputmismach errors. 
        int optionSelected = -1;
        while (optionSelected == -1 && !socket.isClosed()) {
            String optionSelectedString = new String(ReadFromClient());
            if (optionSelectedString.toLowerCase().equals("done")) {
                socket.close();
            }
            try {
                optionSelected = Integer.parseInt(optionSelectedString);
                System.out.println("option: " + optionSelected);
                File fileToSend = getFile(new File(FileDirectory), optionSelected);
                
                FileInputStream fin = new FileInputStream(fileToSend);
                byte[] fileData = new byte[(int) fileToSend.length()];
                
                fin.read(fileData);
                sendToClient(encryptesData(fileData));

//                    Runnable sendFile = new Runnable() {
//                        @Override
//                        public void run() {
//                            sendToClient(encryptesData(getFile(new File(FileDirectory), optionSelected)));
//                        }
//                    };
                optionSelected = -1;
            } catch (NumberFormatException er) {
                break;
            }
        }
    }

    private void KeyCreation() throws NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidKeySpecException {
        /*
         * Alice creates her own DH key pair with 2048-bit key size
         */
        System.out.println("ALICE: Generate DH keypair ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Alice creates and initializes her DH KeyAgreement object
        System.out.println("ALICE: Initialization ...");
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();

        //for debugging purposes, let's print out Alice's encode public key
        System.out.println("Alice's Public Key for Transmit:");
        System.out.println(toHexString(alicePubKeyEnc));

        sendToClient(alicePubKeyEnc);

        clientPubKeyEnc = ReadFromClient();
        KeyFactory aliceKeyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
        PublicKey bobPubKey = aliceKeyFactory.generatePublic(x509KeySpec);
        System.out.println("ALICE: Execute PHASE1 ...");
        aliceKeyAgree.doPhase(bobPubKey, true);

        byte[] aliceSharedSecret = aliceKeyAgree.generateSecret(); // provide output buffer of required size

        System.out.println("The shared key is: " + toHexString(aliceSharedSecret));
        SecretKeySpec bobAESKey = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES");

        try {
            bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        bobCipher.init(Cipher.ENCRYPT_MODE, bobAESKey);
        //send the IV
        sendToClient(bobCipher.getParameters().getEncoded());

    }

    private byte[] encryptesData(byte[] bytestoencrypt) {
        System.out.println("Encrypting");
        byte[] encrypted = null;
        try {
            System.out.println(toHexString(bytestoencrypt));
            encrypted = bobCipher.doFinal(bytestoencrypt);
            System.out.println(toHexString(encrypted));
        } catch (Exception ex) {
            System.out.println("There was a error in encrypting that data");
            System.out.println(ex.toString());
        }
        return encrypted;

    }

    private void sendToClient(byte[] DataToSend) throws IOException {
        int size = DataToSend.length;
        BigInteger bi = BigInteger.valueOf(size);

        System.out.println(toHexString(bi.toByteArray()));
        System.out.println(size);
        if (bi.toByteArray().length < 2) {
            byte[] correctSize = new byte[2];
            correctSize[1] = bi.toByteArray()[0];
            out.write(correctSize);
        } else {
            out.write(bi.toByteArray());
        }
        out.flush();
        out.write(DataToSend, 0, DataToSend.length);
        out.flush();
    }

    private byte[] ReadFromClient() throws IOException {

        byte[] clientData = null;
        BigInteger bi;
        byte[] sizeInBites = new byte[2];
        in.read(sizeInBites);
        //System.out.println("Clients stream Length " + toHexString(sizeInBites));
        bi = new BigInteger(sizeInBites);
        clientData = new byte[bi.intValue()];

        in.read(clientData);
        //System.out.println("Clients key " + toHexString(clientData));
        return clientData;

    }

    /**
     * This code snippet was found here
     * "https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder"
     * edited to meet requriments
     *
     * @return File list with delimiter \n
     */
    private String getFileNames(File folder) {
        //make sure its a directory not a file
        if (folder.isDirectory() == false) {
            return null;
        }

        System.out.println(folder.getAbsolutePath());

        File[] listOfFiles = folder.listFiles();
        String files = "";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                //System.out.println("File " + listOfFiles[i].getName());
                files = files + i + ": " + listOfFiles[i].getName() + "\n";

            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        return files;
    }

    private File getFile(File folder, int index) {
        if (folder.isDirectory() == false) {
            return null;
        }

        System.out.println(folder.getAbsolutePath());

        File[] listOfFiles = folder.listFiles();
        System.out.println(listOfFiles[index].getName());
        return listOfFiles[index];
    }

    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
}
