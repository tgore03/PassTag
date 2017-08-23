package com.example.tanmaysue.passtag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class LoginActivity extends AppCompatActivity {

    private static String email, password, storedemail, storedpassword;

    //copy everywhere
    private static Socket socket;
    private static int port = 8989;
    private static String ip = "54.201.14.113";
    private static SecretKey AESK;
    private static PublicKey PUBK;
    private static DataOutputStream dos;
    private static DataInputStream dis;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.i("a", "\n\nLogin Activity\n");
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText emailEditText = (EditText) findViewById(R.id.EmailEditText);
        final EditText passEditText = (EditText) findViewById(R.id.PasswordEditText);
        Button loginButton = (Button) findViewById(R.id.LoginButton);
        Button registerButton = (Button) findViewById(R.id.RegisterButton);

        /* Generate keys and store in file only if "key" file not stored on internal storage
        * Don't need here
        File file = new File(getFilesDir(), "key");
        if(!file.exists()){
            generateKeys();
        }
        */

        //connect to server
        connectToServer();

        // Implementation of Login Button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = emailEditText.getText().toString();
                password = passEditText.getText().toString();
                Log.i("a", "");
                Log.i("a", "Entered email = " + email + "/Entered Password = " + password);

                if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailEditText.setError("Invalid Email Address");
                } else {

                    try {
                        // readFile();  // If autologin needed remove the comments here
                        if (loginUser(email, password)) {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                        } else {
                            Log.i("a", "login unsuccessful");
                            Toast.makeText(getApplicationContext(), "Login Unsuccessful. Try Again", Toast.LENGTH_SHORT).show();
                            passEditText.setText("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        //Implementation of Register Button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("a", "");
                email = emailEditText.getText().toString();
                password = passEditText.getText().toString();

                if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailEditText.setError("Invalid Email Address");
                } else {

                    if (registerUser(email, password)) {
                        // Store email and password in file for local use
                        if (storeInFile(email, password)) {
                            // Goto Main Activity on successful Registration
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Registration Failed. Try Again", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Registration Unsuccessful. Try Again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        //Initialize App
        if(!initializeApp()){
            Log.i("a", "Fatal Error - Server Public Key not received");
            Toast.makeText(getApplicationContext(), "Server Not Available! Please Try Again Later", Toast.LENGTH_LONG).show();
            System.exit(0);
        }

        //Start autologin
       // autoLogin(); // Start Autologin from here.
    }

    private boolean connectToServer(){
        try{
            //connect to server
            socket = new Socket(ip, port);
            Log.i("a","connected to server");

            // Initialize variables
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            return true;
        }catch (Exception e){
            e.printStackTrace();
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


    // Receive Server's Public key when app starts.
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
    private boolean loginUser(String email, String password){
        try {
            //Specify "login" choice to server
            Log.i("a", "starting login (2)");
            dos.writeInt(2);

            if(!sendUserCredential(email,password)) {
                return false;
            }

            // receive login status from server
            int status = dis.readInt();
            Log.i("a", "Received status from server for autologin = " + status);

            if(status != 0) {
                Log.i("a", "status = " + status);
                return false;
            }
            if(storeInFile(email,password))
                return true;
            else
                return false;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    private boolean registerUser(String email, String password){
        try {
            int length;

            //Specify "register" choice to server
            dos.writeInt(3);

            //Send user credentials to server
            sendUserCredential(email, password);

            // receive login status from server
            int status = dis.readInt();
            Log.i("a", "Received status for register as : " + status);

            //Execute action based on status
            if(status == 0 && storeInFile(email,password)) {
                Log.i("a", "Registration Successful");
                return true;
            }else {
                Log.i("a", "Registration Unsuccessful");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
    private boolean storeInFile(String email, String password){
        // Store email and password in file for local use
        String filename = "credential.txt";
        File file = new File(getApplicationContext().getFilesDir(), filename);
        file.delete();
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = openFileOutput(filename, MODE_PRIVATE);
            fileOutputStream.write(email.getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write(password.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.i("a", "data stored in file");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "data not stored in file");
            return false;
        }
    }
    private boolean storeKeyInFile(SecretKey AESK){
        Log.i("a", "storeKeyInFile");
        String filename = "key";
        File file = new File(getFilesDir(), filename);
        try {
            if(!file.exists()) {
                file.createNewFile();
                FileOutputStream fileOutputStream = openFileOutput(filename, MODE_PRIVATE);
                fileOutputStream.write(AESK.getEncoded());
                fileOutputStream.flush();
                fileOutputStream.close();
                Log.i("a", "key stored in file");
            } else {
                Log.i("a", "Key already exists");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    protected static PublicKey getServerPubicKey(){
        return PUBK;
    }
    protected static String getEmail(){
        return storedemail;
    }
    protected static String getPassword(){
        return storedpassword;
    }
    protected static Socket getSocket(){
        return socket;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //closeSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing())
            closeSocket();
    }


    // Functions not needed here
    private void generateKeys(){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
            keyGenerator.init(128);
            AESK = keyGenerator.generateKey();
            Log.i("a", "Generated AESK : " + Arrays.toString(AESK.getEncoded()));
            storeKeyInFile(AESK);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void autoLogin(){
        try {
            Log.i("a", "");
            Log.i("a", "autoLogin");

            //Read email and password from file
            readFile();

            // initiate the login process only if user account credentials are available
            if (storedemail.length() > 0 & storedpassword.length() > 0) {
                Log.i("a", "stored email and password exist");

                if (loginUser(storedemail, storedpassword)) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                } else {
                    Log.i("a", "login unsuccessful");
                }
            } else {
                Log.i("a", "saved credentials not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "Auto Login failed");
        }
    }

}
