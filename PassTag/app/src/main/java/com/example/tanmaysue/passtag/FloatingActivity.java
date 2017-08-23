package com.example.tanmaysue.passtag;

import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.system.OsConstants;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

public class FloatingActivity extends AppCompatActivity {

    private static String tag, storedemail, storedpassword;
    private static PublicKey PUBK, CPUBK;
    private static PrivateKey CPRIV;
    private static SecretKey AESK;
    private static LoginActivity loginActivity = new LoginActivity();
    private static Socket socket;
    private static int port = 8989;
    private static String ip = "54.201.14.113";
    private static DataOutputStream dos;
    private static DataInputStream dis;
    boolean visible = true;

    private static EditText resultEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floating);

        Log.i("a" ,"Floating Activity");

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // initialize variables
        final EditText tageditText = (EditText) findViewById(R.id.TagEditText);
        Button tagsearchButton = (Button) findViewById(R.id.TagButton);
        resultEditText = (EditText) findViewById(R.id.ResultEditText);

        readKey();
        readFile();

        // Search for Tag
        tagsearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get tag
                tag = tageditText.getText().toString();
                Log.i("a", "tag = " + tag);
                if(tag == null){
                    Toast.makeText(getApplicationContext(), "Enter Tag", Toast.LENGTH_SHORT).show();
                }else {
                    if(resultEditText.getText().toString() != null){
                        resultEditText.setText("");
                        resultEditText.setVisibility(View.INVISIBLE);
                    }

                    connectToServer();
                    generateKeys();
                    //Initialize App
                    if (!initializeApp()) {
                        Log.i("a", "Fatal Error - Server Public Key not received");
                        Toast.makeText(getApplicationContext(), "Server Not Available! Please Try Again Later", Toast.LENGTH_LONG).show();
                        System.exit(0);
                    }
                    retrieveData(getApplicationContext());
                }
            }
        });
    }

    private static void printStatus(int status, Context context) {
        switch (status){
            case 1:
                Toast.makeText(context.getApplicationContext(),"Login Failed", Toast.LENGTH_SHORT ).show();
                break;
            case 2:
                Toast.makeText(context.getApplicationContext(),"Registration Failed", Toast.LENGTH_SHORT ).show();
                break;
            case 3:
                Toast.makeText(context.getApplicationContext(),"Tag Already Exist", Toast.LENGTH_SHORT ).show();
                break;
            case 4:
                Toast.makeText(context.getApplicationContext(),"No Data Available", Toast.LENGTH_SHORT ).show();
                break;
            case 5:
                Toast.makeText(context.getApplicationContext(),"Tag Does Not Exist", Toast.LENGTH_SHORT ).show();
                break;
        }
    }
    private void generateKeys(){
        try {
            if(CPUBK == null || CPRIV == null) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
                SecureRandom secureRandom = new SecureRandom();
                Log.i("a", "random number = " + secureRandom);
                keyPairGenerator.initialize(1024, secureRandom);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                CPUBK = keyPair.getPublic();
                CPRIV = keyPair.getPrivate();
                Log.i("a", "Client Public Private keys created");
                Log.i("a", "CPUBK = " + Arrays.toString(CPUBK.getEncoded()));

            } else {
                Log.i("a", "Client Public Private Keys already exists");
                Log.i("a", "CPUBK = " + Arrays.toString(CPUBK.getEncoded()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean readFile(){
        String data;
        String filename = "credential.txt";

        File file = new File(getFilesDir(), filename);
        try {
            if(file.exists()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                data = bufferedReader.readLine();
                if (data != null)
                    storedemail = data;
                data = bufferedReader.readLine();
                if (data != null)
                    storedpassword = data;
                data = "";

                Log.i("a", "Data read from file. Email = " + storedemail + "/Password = " + storedpassword);
                return true;
            } else {
                Log.i("a", "File does not exists");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean readKey(){
        String filename = "key";
        File file = new File(getFilesDir(), filename);
        try{
            if(file.exists()) {
                //read key file to get AES key
                FileInputStream fileInputStream = openFileInput(filename);
                byte[] CPUBKbytes = new byte[fileInputStream.available()];
                fileInputStream.read(CPUBKbytes);

                AESK = new SecretKeySpec(CPUBKbytes, "AES");
                Log.i("a", "AESK = " + Arrays.toString(AESK.getEncoded()));
                return true;
            } else {
                Log.i("a", "File does not exists");
                Toast.makeText(getApplicationContext(), "You have not saved any Password on the server.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "File does not exists");
            return false;
        }
    }
    private boolean sendUserCredential(String email, String password){
        try {
            int length;
            //Send encrpted account email to server
            Cipher cipher = Cipher.getInstance("RSA", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, PUBK);
            byte[] encstoredemailbytes = cipher.doFinal(email.getBytes());
            length = encstoredemailbytes.length;
            dos.writeInt(length);
            dos.write(encstoredemailbytes);
            dos.flush();
            Log.i("a", "enc email sent to server");
            Log.i("a", "enc email content : " + Arrays.toString(encstoredemailbytes));

            //Send encrypted account password to server
            byte[] encstoredpasswordbytes = cipher.doFinal(password.getBytes());
            length = encstoredpasswordbytes.length;
            dos.writeInt(length);
            dos.write(encstoredpasswordbytes);
            dos.flush();
            Log.i("a", "enc password sent to server");
            Log.i("a", "enc password content : " + Arrays.toString(encstoredpasswordbytes));
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean connectToServer(){
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "Connection to server failed");
            Toast.makeText(getApplicationContext(), "Connection to server failed. Try Again Later.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private boolean closeSocket() {
        try {
            //close socket at server sisde
            dos.writeInt(0);
            dos.close();
            dis.close();
            socket.close();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean initializeApp(){
        try {
            int length;

            // Specify "initialize" choice to server
            Log.i("a", "starting initialize (1)");
            dos.writeInt(1);

            //receive status from Server1
            int status = dis.readInt();
            Log.i("a", "received status = " + status);

            // receive public key
            length = dis.readInt();
            if (length > 0 && status == 0) {
                byte pubkey[] = new byte[length];
                dis.read(pubkey);
                X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(pubkey);
                Log.i("a", "test");
                KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
                PUBK = keyFactory.generatePublic(encodedKeySpec);
                Log.i("a", "received server public key ");
                Log.i("a", "Received PUBK : " + Arrays.toString(PUBK.getEncoded()));
                return true;
            } else {
                Log.i("a", "Fatal Error - Server Public Key not received");
                Toast.makeText(getApplicationContext(), "Server Not Available! Please Try Again Later", Toast.LENGTH_LONG).show();
                System.exit(0);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void retrieveData(Context context){
        try {
            Log.i("a", "retrieveData");

            int length = 0;
            String recemail, recpassword, recnotes;

            //Specify "retrieve data" choice to server
            dos.writeInt(5);

            //Send encrpted account email to server
            sendUserCredential(storedemail,storedpassword);

            //send tag to server
            length = tag.length();
            dos.writeInt(length);
            dos.write(tag.getBytes());
            dos.flush();
            Log.i("a", "Tag sent to server");

            //receive status from server
            int status = dis.readInt();
            Log.i("a", "Login status = " + status);

            //send public key to server
            if(status == 0) {
                byte[] CPUBKbytes;
                CPUBKbytes = CPUBK.getEncoded();
                dos.writeInt(CPUBKbytes.length);
                dos.write(CPUBKbytes);
                dos.flush();
                System.out.println("public key sent as " + Arrays.toString(CPUBKbytes));

                status = 2;
                //receive status from server.
                status = dis.readInt();
                Log.i("a", "data retrieval status = " + status);
                if(status == 0) {
                    //receive result from server
                    length = dis.readInt();
                    Log.i("a", "length of received enc data = " + length);
                    byte[] encbytes = new byte[length];
                    dis.read(encbytes);
                    Log.i("a", "Received enc data as : " + Arrays.toString(encbytes));

                    //decrypt the data with private key
                    Cipher cipher = Cipher.getInstance("RSA", "BC");
                    cipher.init(Cipher.DECRYPT_MODE, CPRIV);
                    Log.i("a", "CPRIV key used before data decryption =" + Arrays.toString(CPRIV.getEncoded()));

                    byte[] encdatabytes = cipher.doFinal(encbytes);
                    System.out.println("decrypted bytes of received data: " + Arrays.toString(encdatabytes));

                    // decrypt the data to get the individual fields
                    Log.i("a", "AES key used before data decryption =" + Arrays.toString(AESK.getEncoded()));
                    cipher = Cipher.getInstance("AES", "BC");
                    cipher.init(Cipher.DECRYPT_MODE, AESK);
                    byte[] databytes = cipher.doFinal(encdatabytes);
                    String data = new String(databytes);
                    Log.i("a", "Received data = " + data);

                    //deconcatenate fields from data
                    String[] fields = data.split("#!#");
                    recemail = fields[0];
                    recpassword = fields[1];
                    recnotes = fields[2];
                    Log.i("a", "received email = " + recemail + " password = " + recpassword + "\nnotes = " + recnotes);

                    //concatenate the data in format to display in edittext
                    data = " Email/Username : \n" + recemail + "\n\n Password : \n" + recpassword + "\n\n Notes : \n" + recnotes;
                    resultEditText.setScroller(new Scroller(getApplicationContext()));
                    resultEditText.setMaxLines(40);
                    resultEditText.setVerticalScrollBarEnabled(true);
                    resultEditText.setMovementMethod(new ScrollingMovementMethod());
                    resultEditText.setText(data);
                    resultEditText.setVisibility(View.VISIBLE);
                    Log.i("a", "data : \n" + data);
                } else {
                    Toast.makeText(getApplicationContext(), "Data Retrieve Failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Login Failed. Try Loging In First.", Toast.LENGTH_SHORT).show();
            }

            closeSocket();
            //print status
            printStatus(status, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onStop() {
        super.onStop();
        closeSocket();
        finishAffinity();
    }
}
