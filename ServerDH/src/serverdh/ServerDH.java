/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverdh;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michael.hoff & Ramsey Kerley
 */
public class ServerDH {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Server server = new Server(5000);
        } catch (IOException ex) {
            Logger.getLogger(ServerDH.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
