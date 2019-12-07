/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverdh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.Port;

/**
 * 
 * @author michael.hoff & Ramsey Kerley & Ramsey Kerley
 */


/**
 * clienthandler is a thread that handles one client
 * on a diffrent thread so that multiple clients can connect to the server.
 * @author michael.hoff & Ramsey Kerley & Ramsey Kerley
 */
public class ClientHandler extends Thread {

    String FileDirectory = "src/serverdh/documents/";

    Socket socket;
    private ServerSocket serverSocket = null;
    private OutputStream out = null;
    private InputStream in = null;
    private int port;

    private byte[] clientPubKeyEnc;
    private Cipher bobCipher = null;

    /**
     * Handles the creation of the client
     * @param serverSocket
     * @param out
     * @param in 
     */
    public ClientHandler(ServerSocket serverSocket,OutputStream out, InputStream in) {
        this.serverSocket = serverSocket;
        this.out = out;
        this.in = in;
    }

    @Override
    public void run() {
        try {

            try {
                KeyCreation();
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            //System.out.println();

            HandleFiles();

            out.close();
            socket.close();
            serverSocket.close();
        } catch (Exception e) {
            //System.out.println("Client disconected");
        }
    }

/**
 * Gives the client the option to choose a file and then sends the file to 
 * the client.
 * @throws IOException 
 */
    public void HandleFiles() throws IOException {
        File directory = new File(FileDirectory);
        //System.out.println(getFileNames(directory));
        String filesList = getFileNames(directory);
        try {
            sendToClient(filesList.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        int optionSelected = -1;
        while (optionSelected == -1) {
            String optionSelectedString = new String(ReadFromClient());
            //System.out.println(optionSelected);
            if (optionSelectedString.toLowerCase().equals("done")) {
                socket.close();
            }
            try {
                optionSelected = Integer.parseInt(optionSelectedString);
                //System.out.println("option: " + optionSelected);
                File fileToSend = getFile(new File(FileDirectory), optionSelected);

                FileInputStream fin = new FileInputStream(fileToSend);
                byte[] fileData = new byte[(int) fileToSend.length()];

                fin.read(fileData);
                sendToClient(encryptesData(fileData));
                optionSelected = -1;
            } catch (NumberFormatException er) {
                break;
            }
        }
    }

    /**
     * KeyCreation handles the mutual key with client using diffie-hellman
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws IOException
     * @throws InvalidKeySpecException 
     */
    private void KeyCreation() throws NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidKeySpecException {
        /*
         * Alice creates her own DH key pair with 2048-bit key size
         */
        //System.out.println("ALICE: Generate DH keypair ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Alice creates and initializes her DH KeyAgreement object
        //System.out.println("ALICE: Initialization ...");
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();

        //for debugging purposes, let's print out Alice's encode public key
       // //System.out.println("Alice's Public Key for Transmit:");
        //System.out.println(toHexString(alicePubKeyEnc));

        sendToClient(alicePubKeyEnc);

        clientPubKeyEnc = ReadFromClient();
        KeyFactory aliceKeyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
        PublicKey bobPubKey = aliceKeyFactory.generatePublic(x509KeySpec);
        //System.out.println("ALICE: Execute PHASE1 ...");
        aliceKeyAgree.doPhase(bobPubKey, true);

        byte[] aliceSharedSecret = aliceKeyAgree.generateSecret(); // provide output buffer of required size

        //System.out.println("The shared key is: " + toHexString(aliceSharedSecret));
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
    /**
     * encryptesData e
     * @param bytestoencrypt data to encrypt
     * @return the encrypted data
     */
    private byte[] encryptesData(byte[] bytestoencrypt) {
        //System.out.println("Encrypting");
        byte[] encrypted = null;
        try {
            System.out.println("unicrpted data to send=> " + toHexString(bytestoencrypt));
            encrypted = bobCipher.doFinal(bytestoencrypt);
            System.out.println("encrypted data=> " + toHexString(encrypted));
        } catch (Exception ex) {
            System.out.println("There was a error in encrypting that data");
            System.out.println(ex.toString());
        }
        return encrypted;

    }
/**
 * Send the data to client
 * @param DataToSend is the data to send
 * @throws IOException 
 */
    private void sendToClient(byte[] DataToSend) throws IOException {
        int size = DataToSend.length;
        BigInteger bi = BigInteger.valueOf(size);

        //System.out.println(toHexString(bi.toByteArray()));
        //System.out.println(size);
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
    /**
     * Reads the data from the client
     * @return
     * @throws IOException 
     */
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

        //System.out.println(folder.getAbsolutePath());

        File[] listOfFiles = folder.listFiles();
        String files = "";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                //System.out.println("File " + listOfFiles[i].getName());
                files = files + i + ": " + listOfFiles[i].getName() + "\n";

            } else if (listOfFiles[i].isDirectory()) {
                //System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        return files;
    }
/**
 * Gets a file from a folder
 * @param folder
 * @param index
 * @return 
 */
    private File getFile(File folder, int index) {
        if (folder.isDirectory() == false) {
            return null;
        }

        //System.out.println(folder.getAbsolutePath());

        File[] listOfFiles = folder.listFiles();
        System.out.println("requested " + listOfFiles[index].getName());
        return listOfFiles[index];
    }
    /**
     * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
     * from dhkey agreement
     * @param b
     * @param buf 
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }
/**
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * from dhkey agreement
 * @param block
 * @return 
 */
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
