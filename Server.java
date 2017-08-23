import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import java.nio.charset.Charset;

/**
 * Created by hschang on 2017/4/23.
 */
public class Server extends Thread {
    private static PublicKey PUBK,CPUBK;
    private static PrivateKey PRIV;
    private static DatabaseManager manager;
    private static DataInputStream dis;
    private static DataOutputStream dos;
    private static ServerSocket serverSocket;
    private static Socket clientSocket;


    String email,password,tag,store_data,retrieve_data="";
    int ID,code,status=-1;
    private int senduserid = -1, recuserid = -1;
    static byte[] store_databytes, retrieve_databytes;

    // constructor
    public Server()
            throws ClassNotFoundException, IOException, SQLException,
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        // System.out.println("exit.");
        // manager.end();
    }
    
        public static void main(String[] args) {
        //init
    	try{
            Security.addProvider(new BouncyCastleProvider());
            generateKeys();
            
            int ServerPort = 7788;
            serverSocket = new ServerSocket(ServerPort);
        } catch(Exception e){
            System.err.println(e.getMessage());
        }
	        
//        System.out.println("Start the program....");
    	while(true){
	    try {
	        //new Server();
                
                // connection
                System.out.println("waiting for client at 7788..");
                clientSocket = serverSocket.accept();
                System.out.println("client connected");
                Thread thread = new Server();
                thread.start();
            } catch (Exception e) {
	        System.err.println(e.getMessage());
	        try {
                    dis.close();
                    dos.close();
                    clientSocket.close();
                } catch (IOException ee){
                    System.err.println(e.getMessage());
                }
	    }
    		// cleanConnection();
    	}
    }

    @Override
    public void run() {
        super.run(); //To change body of generated methods, choose Tools | Templates.
        System.out.println("Thread Started");

        // initial userid (-1 => not logged in)
        recuserid = -1;
        senduserid = -1;
        
        try {
            // get I/O streams
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());

            // database manager
            manager = new DatabaseManager();
            manager.connection();

//            System.out.println("manager connected.");

            int length = 0;
            byte[] encemailbytes, encpasswordbytes, emailbytes, passwordbytes,tagbytes,encdatabytes,databytes;

            while(true) {
                System.out.println("Listening to code...");
                //receieve action code
                code = dis.readInt();
                System.out.println("code="+code);
                System.out.println();

                if(code==0)
                    break;

                //receieve accountName, password
                if(code == 2 || code == 3 || code == 5 ){
                    //Receive encrypted email from client
                    length = dis.readInt();
                    if(length > 0){
                        encemailbytes = new byte[length];
                        dis.read(encemailbytes);
                        //          System.out.println("enc email of "+ length +"bytes received : " + Arrays.toString(encemailbytes));

                        //decrypt account email
                        email = RSAdecryptString(encemailbytes);
                        System.out.println("Received email = " + email);
                    }

                    //Receive encrypted password from client
                    length = dis.readInt();
                    if(length > 0){
                        encpasswordbytes = new byte[length];
                        dis.read(encpasswordbytes);
                        // System.out.println("enc password received : " + Arrays.toString(encpasswordbytes));

                        //decrypt account password
                        password = RSAdecryptString(encpasswordbytes);
                        System.out.println("Received password = " + password);
                    }
                }

                Choice(code);// every operation sent status code

                // send retrieve_data if retrieve
                // length = 0;
                if( !(retrieve_databytes == null)){
                    System.out.println("Send un encrypted retrieve_databytes from database= \n"+ Arrays.toString(retrieve_databytes));
                    sendEncString(retrieve_databytes);
                    retrieve_databytes= null;
                }
                status=-1;
                email="";
                password="";
            }

            //end connection
            dis.close();
            dos.close();
            clientSocket.close();

            System.out.println("server side thread stopped");
            System.out.println("Waiting for client to connect..");
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateKeys() throws NoSuchPaddingException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PUBK = keyPair.getPublic();
            PRIV = keyPair.getPrivate();
            //System.out.println("PRIV= "+Arrays.toString(PRIV.getEncoded() ) );
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendByte(byte[] send) throws IOException {
        int length = send.length;
        System.out.println("length: "+length);
        dos.writeInt(length);
        dos.write(send);
        dos.flush();
        System.out.println("sent byte: \n" + Arrays.toString(send));
    }

    private void sendEncString(byte[] s) throws IOException,NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, CPUBK);
        byte[] enc = cipher.doFinal(s);
        sendByte(enc);
    }

    private void sendPUBK() throws IOException {
        System.out.println("Sending PUBK. ");
    	sendByte(PUBK.getEncoded());
    }

    private byte[] receiveByte(int length) throws IOException {
    	byte[] receivedByte;
        receivedByte = new byte[length];
        dis.read(receivedByte);
        System.out.println("receivedByte length "+ length +"bytes received : " + Arrays.toString(receivedByte));

        return receivedByte;
    }

    private String RSAdecryptString(byte[] enc) 
    						throws NoSuchAlgorithmException, InvalidKeyException, 
    						IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException {
    	String dec;
    	byte[] decByte;

        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.DECRYPT_MODE, PRIV);
        decByte = cipher.doFinal(enc);
        System.out.println("rsa decrypted bytes received : " + Arrays.toString(decByte));
        dec = new String(decByte);
//        System.out.println("Received byte to string= " + dec);
        System.out.println();

        return dec;
    }

    private void receivedEncData() throws IOException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException {
        //Receive encrypted data from client
        int length = dis.readInt();
        int length1 = dis.readInt();
        if(length > 0){
            byte[] encdatabytes = new byte[length];
            dis.read(encdatabytes);
            System.out.println("enc data received as : \n" + Arrays.toString(encdatabytes));
            System.out.println();

            //decrypt data
            //store_data = RSAdecryptString(encdatabytes);
            Cipher cipher = Cipher.getInstance("RSA", "BC");
            cipher.init(Cipher.DECRYPT_MODE, PRIV);
            byte[] databytes = cipher.doFinal(encdatabytes);
            System.out.println("rsa decrypted data bytes received : " + Arrays.toString(databytes));

            // Deconcatenate data
            byte[] splittagbyte = ByteUtils.subArray(databytes, 0, length1);
            System.out.println("tag bytes content : " + Arrays.toString(splittagbyte));
            store_databytes = ByteUtils.subArray(databytes, length1, databytes.length);
            tag = new String(splittagbyte);
            //store_data = new String(splitdatabyte, Charset.forName("UTF-8"));
            System.out.println("received tag = " + tag + "\ndata = " + store_databytes);
            System.out.println("length of stored data= "+ store_databytes.length);
        }
    }
    
    private void Choice(int code) throws SQLException, IOException, NoSuchAlgorithmException, InvalidKeyException,
    IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException,InvalidKeySpecException{
        switch(code) {
            case 1: // send public key
                sendPUBK();
                status=0;
                break;

            case 2: // login
                clientLogin();
                break;

            case 3: // register
                register();
                break;

            case 4: // store data
                storeData();
                break;

            case 5: // retrieve
                retrieveData();
                break;

            case 6: // edit
                editData();
                break;
        }
        // 0 = success,
        // 1 = login fail,
        // 2 = register fail,
        // 3 = the tag already exist
        // 4 = empty string, no string to retrieve
        // 5 = the tag not exist
        // -1 = FATAL ERROR (means that we don't know why =P)

        // send status code
        dos.writeInt(status);
        dos.flush();
        System.out.println("status= "+status);

        //send userid to server1
        if(status == 0 && (code == 2 || code == 3)){
            dos.writeInt(senduserid);
            dos.flush();
            System.out.println("sent user id= "+senduserid);
        }
    }

    private void clientLogin() throws SQLException, IOException {
        ID = manager.loginCheck(email, password);
        if(ID!=0) {
            status = 0;
            senduserid = ID;
        }
        else {
            status = 1; // login fail
            senduserid = -1;
        }
    }

    private void register() throws SQLException, IOException {
        ID = manager.register(email, password);
        if(ID!=0) {
            status = 0;
            senduserid = ID;
        }
        else {
            status = 2;// account name exists
            senduserid = -1;
        }
    }

    private void storeData() throws SQLException, IOException, NoSuchAlgorithmException,InvalidKeyException,
    IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException,InvalidKeySpecException{
        ID = dis.readInt();
        System.out.println("Store data");
        System.out.println("ID received = " + ID);

        //Receive encrypted data from client, tag and storedata is stored
        System.out.println("receivedEncData tag+AES[data]");
        receivedEncData();

        if(ID!=0){
            if(manager.storeData(ID, tag, store_databytes))
                status = 0;
            else
                status = 3; //tag already exist
        }
        else{
            System.out.println("cannot retrieve. credential failure");
            status = 1;
        }
    }

    private void retrieveData() throws SQLException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException,InvalidKeySpecException {
        System.out.println("Retrieve data");
        
        //Receive tag from client
        int length = dis.readInt();
        if(length > 0) {
            byte[] tagbytes = new byte[length];
            dis.read(tagbytes);
            tag = new String(tagbytes);
            System.out.println("Received tag = " + tag);
        }
        
        // Check login credentials and send status
        ID = manager.loginCheck(email, password);
        System.out.println("ID= "+ID);
        if(ID!=0){
           status = 0;
        } else {
            status = 1;
        }
        dos.writeInt(status);
        dos.flush();
        System.out.println("status = " + status);
        
        //receive Public key bytes from Server 1
        if (status == 0) {
            length = dis.readInt();
            byte pubkey[] = new byte[length];
            dis.read(pubkey);
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(pubkey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            CPUBK = keyFactory.generatePublic(encodedKeySpec);
            System.out.println("Received CPUBK : " + Arrays.toString(CPUBK.getEncoded()));
        }

        if(ID!=0){
            retrieve_databytes = manager.retrieve(ID, tag);
            System.out.println("ID= "+ ID);
            System.out.println("tag= "+ tag);
            System.out.println("Un encrypted data from database in byte= \n"+ Arrays.toString(retrieve_databytes));
            System.out.println("lenght of Un encrypted data from database= "+ retrieve_databytes.length);
            if( !(retrieve_databytes.length == 1)){
                status = 0;
            }
            else
                status = 4; // empty string, no string to retrieve
        }
        else{
            System.out.println("cannot retrieve. credential failure");
            status = 1;
        }
    }

    private void editData() throws SQLException, IOException, NoSuchAlgorithmException,InvalidKeyException,
            IllegalBlockSizeException, NoSuchProviderException, BadPaddingException, NoSuchPaddingException,InvalidKeySpecException {
        ID = dis.readInt();
        System.out.println("Edit data");
        System.out.println("ID received = " + ID);
        //Receive encrypted data from client, tag and storedata is stored
        receivedEncData();

        if(ID!=0){
            if(manager.editData(ID, tag, store_databytes))
                status = 0;
            else
                status = 5; // The tag not exist!
        }
        else{
            System.out.println("cannot edit. credential failure");
            status = 1;
        }
    }
}
