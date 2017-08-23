
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import javax.sound.midi.Soundbank;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 *
 * @author tgore03
 */
public class Server1 extends Thread {
    
    static private int port = 8989, port2 = 7788;
    static private String ip2 = "172.30.2.11";
    private static ServerSocket serverSocket;
    private static Socket socket1, socket2;
    private static PublicKey PUBK,CPUBK;
    private static PrivateKey PRIV;
    
    private static DataOutputStream dos, dos2;
    private static DataInputStream dis, dis2;

    private static int userid;
    
    public static void main(String args[]){
        
        try {
            
            // Initialize the server
            Security.addProvider(new BouncyCastleProvider());
            generateKeys();
            
            //create socket & when client connected create thread to handle the processing
            serverSocket = new ServerSocket(port);
            while(true) {
                    System.out.println("Waiting for client to connect at " + port);
                    socket1 = serverSocket.accept();
                    System.out.println("Client connected");
                    Thread thread = new Server1();
                    thread.start();
            }
        } catch (Exception ex) {
                Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    @Override
    public void run() {
        super.run(); //To change body of generated methods, choose Tools | Templates.
        System.out.println("Thread Started");

        // initial userid (-1 => not logged in)
        userid = -1;
        try {
            while(true){
                
                //create socket & connect to Server2 socket
                socket2 = new Socket(ip2, port2);
                System.out.println("Connected to private subnet");
            
                //initialize connection parameters
                dos2 = new DataOutputStream(socket2.getOutputStream());
                dis2 = new DataInputStream(socket2.getInputStream());
            
                // initialize variables
                int choice = 0;
                dis = new DataInputStream(socket1.getInputStream());
                dos = new DataOutputStream(socket1.getOutputStream());

                //Read choice of action
                System.out.println("");
                System.out.println("Waiting for choice from client");
                choice = dis.readInt();
                System.out.println("Choice = " + choice);

                // close the connection when requested
                if(choice == 0)
                    break;
                executeChoice(choice);
            }
            
            //end connection
            dis.close();
            dos.close();
            socket1.close();
            
            dos2.writeInt(0); // end server side connection
            dos2.close();
            dis2.close();
            socket2.close();
            System.out.println("server & server1 side socket closed and thread stopped");
            System.out.println("Waiting for client to connect at " + port);
        } catch (IOException ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void executeChoice(int choice){
        try {
            switch(choice){
                case 1: 
                    initConnection();
                    break;
                
                case 2: 
                    clientLogin();
                    break;
                
                case 3: 
                    clientRegister();
                    break;
                    
                case 4:
                    uploadData();
                    break;
                
                case 5:
                    retrieveData();
                    break;

                case 6:
                    editData();
                    break;
                    
                default: 
                    System.out.println("Invalid input");
                    break;
            }
        } catch (Exception e) {
        }
    }
    
    
    //Generate key pair when server starts.
    private static void generateKeys(){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PUBK = keyPair.getPublic();
            PRIV = keyPair.getPrivate();
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void initConnection(){
        try {
            System.out.println("");
            System.out.println("initConnection");
            
            //initialize variables
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            
            //specify choice for initialization to Server2
            dos2.writeInt(1);
            
            //receive Public key bytes from Server2
            length = dis2.readInt();
            byte[] PUBKbytes = new byte[length];
            dis2.read(PUBKbytes);
            System.out.println("received PUBKbytes = " + Arrays.toString(PUBKbytes));
            
            //receive status from Server2 and send to client
            int status = dis2.readInt();
            System.out.println("received status = " + status);
            dos.writeInt(status);
            
            //Send public key to client
            if(length > 0 && status == 0){
                dos.writeInt(length);
                dos.write(PUBKbytes);
                dos.flush();
                System.out.println("public key sent");
            }
            
            /*
            // Send Public Key
            if(PUBK.getEncoded() != null) {
                length = PUBK.getEncoded().length;
                dos.writeInt(length);
                dos.write(PUBK.getEncoded());
                dos.flush();
                System.out.println("public key sent");
                System.out.println("PUBK : " + Arrays.toString(PUBK.getEncoded()));
            }
            */
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }     
    }
    
    private static void clientLogin(){
        try {
            System.out.println("");
            System.out.println("clientLogin");
            
            // Initialize
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            String email, password;
            int status;
            byte[] encemailbytes, encpasswordbytes, emailbytes, passwordbytes;
            
            //specify choice for clientLogin to Server2
            dos2.writeInt(2);
            
            //Receive encrypted email from client
            length = dis.readInt();
            if(length > 0){
                encemailbytes = new byte[length];
                dis.read(encemailbytes);
                System.out.println("enc email of "+ length +"bytes received : " + Arrays.toString(encemailbytes));
                
                //Send received encrypted email to Server2
                dos2.writeInt(length);
                dos2.write(encemailbytes);
                System.out.println("enc email bytes sent to Server2");
            }
            
            //Receive encrypted password from client
            length = dis.readInt();
            if(length > 0){
                encpasswordbytes = new byte[length];
                dis.read(encpasswordbytes);
                System.out.println("enc password received : " + Arrays.toString(encpasswordbytes));
                
                //Send received encrypted password to Server2
                dos2.writeInt(length);
                dos2.write(encpasswordbytes);
                dos2.flush();
                System.out.println("enc password bytes sent to Server2");
            }
            
            
            // Receive status from Server2 & send to client
            status = dis2.readInt();
            System.out.println("Status = " + status);
            
            //  Receive userid from Server2 and store it
            if(status==0){
                userid = dis2.readInt();
                System.out.println("userid = " + userid);
            }

            dos.writeInt(status);
            dos.flush();
            System.out.println("status sent back for clientLogin as : " + status);
            
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void clientRegister(){
        try {
            System.out.println("");
            System.out.println("clientRegister");
            
            // Initialize
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            String email, password;
            int status;
            byte[] encemailbytes, encpasswordbytes, emailbytes, passwordbytes;
            
            //specify choice for client register to Server2
            dos2.writeInt(3);
            
            //Receive encrypted email from client
            System.out.println("waiting for account % password... ");
            length = dis.readInt();
            System.out.println("enc email length : " + length);
            if(length > 0){
                encemailbytes = new byte[length];
                dis.read(encemailbytes);
                System.out.println("enc email received as : " + Arrays.toString(encemailbytes));
                
                //Send received encrypted email to Server2
                dos2.writeInt(length);
                dos2.write(encemailbytes);
                System.out.println("enc email bytes sent to Server2");
                
                /*
                //decrypt account email
                Cipher cipher = Cipher.getInstance("RSA", "BC");
                cipher.init(Cipher.DECRYPT_MODE, PRIV);
                emailbytes = cipher.doFinal(encemailbytes);
                email = new String(emailbytes);
                System.out.println("Received email = " + email);
                */
            }
            
            //Receive encrypted password from client
            length = dis.readInt();
            if(length > 0){
                encpasswordbytes = new byte[length];
                dis.read(encpasswordbytes);
                System.out.println("enc password received as : " + encpasswordbytes);
                
                //Send received encrypted password to Server2
                dos2.writeInt(length);
                dos2.write(encpasswordbytes);
                dos2.flush();
                System.out.println("enc password bytes sent to Server2");
                
                /*
                //decrypt account password
                Cipher cipher = Cipher.getInstance("RSA","BC");
                cipher.init(Cipher.DECRYPT_MODE, PRIV);
                passwordbytes = cipher.doFinal(encpasswordbytes);
                password = new String(passwordbytes);
                System.out.println("Received password = " + password);
                */
            }
            
            
            // Receive status from Server2 & send to client
            status = dis2.readInt();

            //  Receive userid from Server2 and store it
            if(status==0)
                userid = dis2.readInt();
            System.out.println("userid = " + userid);

            dos.writeInt(status);
            dos.flush();
            System.out.println("status sent back for clientRegister as : " + status);
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void uploadData(){
        try {
            System.out.println("");
            System.out.println("uploadData");
            
            // Initialize
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            String tag, data;
            int status;
            byte[] encdatabytes, databytes;
            
            //specify choice for initialization to Server2
            dos2.writeInt(4);

            // send Server 2 user id
            dos2.writeInt(userid);
            
            //Receive lenghts and encrypted data from client
            length = dis.readInt();
            int length1 = dis.readInt();
            if(length > 0){ 
                encdatabytes = new byte[length];
                dis.read(encdatabytes);
                System.out.println("enc data received as : " + Arrays.toString(encdatabytes));
                
                //Send received encrypted data to Server2
                dos2.writeInt(length);
                dos2.writeInt(length1);
                dos2.write(encdatabytes);
                dos2.flush();
                        
                // Receive status from Server2 & send to client
                status = dis2.readInt();
                dos.writeInt(status);
                dos.flush();
                System.out.println("status sent back for uploadData as : " + status);
            }
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void retrieveData(){
        try{
            System.out.println("");
            System.out.println("retrieveData");
            
            // Initialize
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            String tag;
            int status;
            byte[] encemailbytes, encpasswordbytes, emailbytes, passwordbytes, tagbytes, databytes;
            
            //Specify "retrievedata" choice to server2
            dos2.writeInt(5);
            dos2.flush();
            
            //Receive encrypted email from client
            length = dis.readInt();
            if(length > 0){
                encemailbytes = new byte[length];
                dis.read(encemailbytes);
                System.out.println("enc email received as : " + Arrays.toString(encemailbytes));
                
                //Send received encrypted email to Server2
                dos2.writeInt(length);
                dos2.write(encemailbytes);
                dos2.flush();
                System.out.println("enc email bytes sent to Server2");   
            }
            
            //Receive encrypted password from client
            length = dis.readInt();
            if(length > 0){
                encpasswordbytes = new byte[length];
                dis.read(encpasswordbytes);
                System.out.println("enc password received as : " + encpasswordbytes);
                
                //Send received encrypted password to Server2
                dos2.writeInt(length);
                dos2.write(encpasswordbytes);
                dos2.flush();
                System.out.println("enc password bytes sent to Server2");
            }
            
            //Receive tag from client
            length = dis.readInt();
            if(length > 0) {
                tagbytes = new byte[length];
                dis.read(tagbytes);
                tag = new String(tagbytes);
                System.out.println("Received tag = " + tag);
                
                //send tag to Server2
                dos2.writeInt(length);
                dos2.write(tagbytes);
                dos2.flush();
                System.out.println("Tag sent to server 2");

                //receive status from Server2 and send to client
                status = dis2.readInt();
                dos.writeInt(status);
                dos.flush();
                System.out.println("login status = " + status);
                
                if(status == 0){
                    //receive public key bytes from client and send to server 2
                    length = dis.readInt();
                    byte[] keybytes = new byte[length];
                    dis.read(keybytes);
                    dos2.writeInt(length);
                    dos2.write(keybytes);
                    dos2.flush();
                    System.out.println("publilc key received as " + Arrays.toString(keybytes));
                    
                    //receive status from server2 and send to client
                    status = dis2.readInt();
                    dos.writeInt(status);
                    dos.flush();
                    System.out.println("Retrieve status = " + status);
                    
                    //receive databytes from server 2 and send to client
                    if(status == 0) {
                        length = dis2.readInt();
                        databytes = new byte[length];
                        dis2.read(databytes);
                        //send databytes to client
                        dos.writeInt(length);
                        dos.write(databytes);
                        dos.flush();
                        System.out.println("data received from server 2 as " + Arrays.toString(databytes));
                    }
                } else {
                        //send status to client
                        dos.writeInt(status);
                        dos.flush();
                        System.out.println("retrieve status = " + status);
                    }
            }
        } catch(Exception ex){
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void editData(){
        try {
            System.out.println("");
            System.out.println("editData");

            // Initialize
            dos = new DataOutputStream(socket1.getOutputStream());
            dis = new DataInputStream(socket1.getInputStream());
            int length = 0;
            String tag, data;
            int status;
            byte[] encdatabytes, databytes;

            //specify choice for initialization to Server2
            dos2.writeInt(6);

            // send Server 2 user id
            dos2.writeInt(userid);

            //Receive lengths and encrypted data from client
            length = dis.readInt();
            int length1 = dis.readInt();
            if(length > 0){
                encdatabytes = new byte[length];
                dis.read(encdatabytes);
                System.out.println("enc data received as : " + Arrays.toString(encdatabytes));

                //Send received encrypted data to Server2
                dos2.writeInt(length);
                dos2.writeInt(length1);
                dos2.write(encdatabytes);
                dos2.flush();

                // Receive status from Server2 & send to client
                status = dis2.readInt();
                dos.writeInt(status);
                dos.flush();
                System.out.println("status sent back for editData as : " + status);
            }
        } catch (Exception ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
