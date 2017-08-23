package com.example.tanmaysue.passtag;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private static String tag, email, password, notes, useremail, userpassword;
    //private static String ip = "52.36.80.217";
    private static int port = 8989;
    private static Socket socket;
    private static DataOutputStream dos;
    private static DataInputStream dis;
    private static PublicKey PUBK;
    private static SecretKey AESK;
    private static LoginActivity loginActivity = new LoginActivity();
    private static EditText enterTagEditText, enterEmailEditText, enterPasswordEditText, enterNoteEditText;

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.i("a", "Main Activity Started");
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            // Start the service
            Intent intent = new Intent(this, PassTagService.class);
            startService(intent);
        }





        enterTagEditText = (EditText) findViewById(R.id.EnterTagEditText);
        enterEmailEditText = (EditText) findViewById(R.id.EnterEmailEditText);
        enterPasswordEditText = (EditText) findViewById(R.id.EnterPasswordEditText);
        enterNoteEditText = (EditText) findViewById(R.id.EnterNoteEditText);
        CheckBox showPasswordCheckBox= (CheckBox) findViewById(R.id.showPasswordCheckBox);

        loadVariables();
        readKey();

        // Implement floating button listener to store data to cloud
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tag = enterTagEditText.getText().toString();
                email = enterEmailEditText.getText().toString();
                password = enterPasswordEditText.getText().toString();
                notes = enterNoteEditText.getText().toString();
                Log.i("a", "Entered Details: Tag = " + tag + "  email = " + email
                        + "  password = " + password + "notes = " + notes);

                // Send data to cloud
                if(socket.isConnected()) {
                    if (sendData(4, tag, email, password, notes)) {
                        // on success clear data on activity
                        enterTagEditText.setText("");
                        enterEmailEditText.setText("");
                        enterPasswordEditText.setText("");
                        enterNoteEditText.setText("");
                        Toast.makeText(getApplicationContext(), "Data Successfully Saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Data Not Saved. Try Again", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("a", "socket not connected");
                    Toast.makeText(getApplicationContext(), "Connection to Server Failed. Try Again Later", Toast.LENGTH_SHORT).show();
                    System.exit(0);
                }
            }
        });

        // Implement floating button listener to store data to cloud
        FloatingActionButton fabedit = (FloatingActionButton) findViewById(R.id.floatingActionButtonEdit);
        fabedit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tag = enterTagEditText.getText().toString();
                email = enterEmailEditText.getText().toString();
                password = enterPasswordEditText.getText().toString();
                notes = enterNoteEditText.getText().toString();
                Log.i("a", "Edit data - Entered Details: Tag = " + tag + "  email = " + email
                        + "  password = " + password + "notes = " + notes);

                // Edit data to cloud
                if(socket.isConnected()) {
                    if (sendData(6, tag, email, password, notes)) {
                        // on success clear data on activity
                        enterTagEditText.setText("");
                        enterEmailEditText.setText("");
                        enterPasswordEditText.setText("");
                        enterNoteEditText.setText("");
                        Toast.makeText(getApplicationContext(), "Data Successfully Edited", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Data Not Edited. Try Again", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("a", "socket not connected");
                    Toast.makeText(getApplicationContext(), "Connection to Server Failed. Try Again Later", Toast.LENGTH_SHORT).show();
                    System.exit(0);
                }
            }
        });

        // Implement checkbox listener to display password to user
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.i("a", "check box checked");
                    enterPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }else{
                    Log.i("a", "check box not checked");
                    enterPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

    }

    private boolean loadVariables(){
        try {
            Log.i("a","");
            Log.i("a", "loadVariables");
            PUBK = loginActivity.getServerPubicKey();
            Log.i("a", "Pubic Key PUBK = " + Arrays.toString(PUBK.getEncoded()));
            useremail = loginActivity.getEmail();
            Log.i("a", "user email = " + useremail);
            userpassword = loginActivity.getPassword();
            Log.i("a", "user password = " + userpassword);
            socket = loginActivity.getSocket();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "load variables failed");
            return false;
        }
    }
    private static boolean sendData(int choice, String tag, String email, String password, String notes){
        try {
            Log.i("a", "sendData");

            int length;
            //Specify "senddata" choice to server
            dos.writeInt(choice);

            //Concatenate email, password and notes data and encrypt using AESK
            String concat = email + "#!#" + password + "#!#" + notes;
            Log.i("a", "concat data = " + concat);

            Cipher cipher = Cipher.getInstance("AES", "BC");
            Log.i("a", "AESK = " + Arrays.toString(AESK.getEncoded()));
            cipher.init(Cipher.ENCRYPT_MODE, AESK);
            byte[] encconcatbytes = cipher.doFinal(concat.getBytes());
            Log.i("a", "concat bytes with aes enc = " + Arrays.toString(encconcatbytes));

            cipher.init(Cipher.DECRYPT_MODE, AESK);
            byte[] temp = cipher.doFinal(encconcatbytes);
            Log.i("a", "temp = " + new String(temp));

            //concatenate Tag to it.
            byte[] tagbyte = tag.getBytes();
            Log.i("a", "tagbytes = " + Arrays.toString(tagbyte));
            byte[] databyte = new byte[tagbyte.length + encconcatbytes.length];
            System.arraycopy(tagbyte, 0, databyte, 0, tagbyte.length);
            System.arraycopy(encconcatbytes, 0, databyte, tagbyte.length, encconcatbytes.length);
            Log.i("a", "databyte before rsa encryption = " + Arrays.toString(databyte));

            //Encrypt with RSA Key
            cipher = Cipher.getInstance("RSA","BC");
            cipher.init(Cipher.ENCRYPT_MODE, PUBK);
            byte[] encdatabyte = cipher.doFinal(databyte);
            Log.i("a","rsa enc databyte = " + Arrays.toString(encdatabyte));

            //Send encrypted data to server
            length = encdatabyte.length;
            int length1 = tagbyte.length;
            dos.writeInt(length);
            dos.writeInt(length1);
            dos.write(encdatabyte);
            dos.flush();
            Log.i("a", "enc data sent to server");

            // Receive status
            int status = dis.readInt();
            Log.i("a","received status for send data as : " + status);

            //Execute action based on status
            if(status == 0){
                Log.i("a", "Data Saved/Edited Successful");
                return true;
            } else {
                Log.i("a", "Data Not Saved/Edited");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
    private boolean generateKeys(){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
            keyGenerator.init(128);
            AESK = keyGenerator.generateKey();
            Log.i("a", "Generated AESK : " + Arrays.toString(AESK.getEncoded()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean storeKeyInFile(SecretKey AESK){
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
    private boolean readKey(){
        Log.i("a", "readKey");
        String filename = "key";
        File file = new File(getFilesDir(), filename);
        try{
            if(file.exists()) {
                // Read AES key from file if file exists.
                FileInputStream fileInputStream = openFileInput(filename);
                byte[] CPUBKbytes = new byte[fileInputStream.available()];
                fileInputStream.read(CPUBKbytes);

                AESK = new SecretKeySpec(CPUBKbytes, "AES");
                Log.i("a", "readKey() in Main AESK = " + Arrays.toString(AESK.getEncoded()));
                return true;
            } else if (generateKeys() && storeKeyInFile(AESK)) {
                // else generate AES key and store in the file. If storing in file fails, return.
                Log.i("a", "file do not exist. Key generated and stored in file");
                return true;
            } else {
                Log.i("a", "Key File not created");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("a", "File does not exists");
            return false;
        }
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                // Start the service
                Intent intent = new Intent(this, PassTagService.class);
                startService(intent);
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(socket.isConnected())
            closeSocket();
        System.exit(0);
    }
}
