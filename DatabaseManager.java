// package databasepkg;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


public class DatabaseManager
{
  // The JDBC Connector Class.
  private static final String dbClassName = "com.mysql.jdbc.Driver";

  // Connection string. mypassDB is the database the program
  // is connecting to. You can include user and password after this
  // by adding (say) ?user=paulr&password=paulr. Not recommended!

  private final String CONNECTION =
                          "jdbc:mysql://id-passdb.cyexrsggmza6.us-west-2.rds.amazonaws.com/mypassDB";
  private Connection c;
  private Statement st;

  public void connection() throws ClassNotFoundException, SQLException {
    System.out.println(dbClassName);
    // Class.forName(xxx) loads the jdbc classes and
    // creates a drivermanager class factory
    Class.forName(dbClassName);

    // Properties for user and password.
    Properties p = new Properties();
    p.put("user","hschang");
    p.put("password","idon'tknow");

    // Now try to connect
    System.out.println("connecting to the database..");
    c = DriverManager.getConnection(CONNECTION,p);

    System.out.println("Database connected!");
  }

  public void end() throws SQLException{
    // close statement
    if(st!=null)
      st.close();
    // close the database connection
    if(c!=null)
      c.close();
  }

    /*
    *  Check if the user is valid
    */
    public int loginCheck(String username, String password) throws SQLException {
      String query = "SELECT * FROM Users WHERE username='"+username+"' AND password='"+password+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
     
      return rs.next()? rs.getInt("ID") : 0;
    }

    /*
    *  insert data into table Credentials
    */
    private boolean insertData(int id, String tag, byte[] data) throws SQLException {
      String query = "INSERT INTO Credentials (tag, data, userid)"
        + " values (?, ?, ?)";
      
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
      // create the mysql insert preparedstatement
      PreparedStatement preparedStmt = c.prepareStatement(query);
      preparedStmt.setString (1, tag);
      preparedStmt.setBinaryStream(2, byteArrayInputStream);
      preparedStmt.setInt    (3, id);

      System.out.println("data inserting..");
      // execute the preparedstatement
      return preparedStmt.execute();
    }

    /*
    *  edit data in table Credentials by specifying correct id and tag
    */
    // action:?
    public boolean editData(int id, String tag, byte[] data) throws SQLException{
      // check tag existence
      String query = "SELECT * FROM Credentials WHERE userid="+id+" AND tag='"+tag+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      if (!rs.next()) {
        System.err.println("The tag not exist!");
        return false;
      }
      
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
      // create the java mysql update preparedstatement
      query = "UPDATE Credentials SET data = ? WHERE userid = ? AND tag= ? ";
      PreparedStatement preparedStmt = c.prepareStatement(query);
      preparedStmt.setBinaryStream(1, byteArrayInputStream);
      preparedStmt.setInt   (2, id);
      preparedStmt.setString(3, tag);

      // execute the java preparedstatement
      preparedStmt.executeUpdate();
      System.out.println("data edited");
      return true;
    }

    /*
    *  register account for the app
    */
    // action:3
    public int register(String username, String password) throws SQLException {
      // check username existence
      String query = "SELECT * FROM Users WHERE username='"+username+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      if (rs.next()) {
        System.err.println("The username exists.");
        return 0;
      }

      // the mysql insert statement
      query = "INSERT INTO Users (username, password)"
        + " values (?, ?)";

      // create the mysql insert preparedstatement
      PreparedStatement preparedStmt = c.prepareStatement(query);
      preparedStmt.setString (1, username);
      preparedStmt.setString (2, password);

      // execute the preparedstatement
      preparedStmt.execute();
      System.out.println("User registered.");

      query = "SELECT ID FROM Users WHERE username='"+username+"'";
      st = c.createStatement();
      rs = st.executeQuery(query);

      // return id
      //System.out.println("ID= "+ID);
        return rs.next()? rs.getInt("ID"): 0;
    }

    /*
    *  store data in table Credentials by specifying correct id 
    */
    // action:4
    public boolean storeData(int id, String tag, byte[] data) throws SQLException {
      // check tag existence
      String query = "SELECT * FROM Credentials WHERE userid="+id+" AND tag='"+tag+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      if (rs.next()) {
        System.err.println("The tag already exist! data not stored");
        return false;
      }

      insertData(id, tag, data);
      return true;
    }

    /*
    *  retrieve data in table Credentials if having correct id and tag
    */
    // action:5
    public byte[] retrieve(int id, String tag) throws SQLException {
      String query = "SELECT * FROM Credentials WHERE userid="+id+" AND tag='"+tag+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      if(rs.next()){    
        byte[] data = rs.getBytes("data");
        System.out.println("length of data retrieved from database = " + data.length);
        return data;
      } else {
          byte[] data = {0};
          System.out.println("length of data when retrieve from database failed = " + data.length);
          return data;
      }
    }

//**** for testing purpose ****

    /*
    *  given id, check the row data in table Users  
    */
    private void selectUsers(int id) throws SQLException{
      // our SQL SELECT query. 
      // if you only need a few columns, specify them by name instead of using "*"
      String query = "SELECT * FROM Users WHERE ID="+id;

      // create the java statement
      st = c.createStatement();
      
      // execute the query, and get a java resultset
      ResultSet rs = st.executeQuery(query);
      
      // iterate through the java resultset
      while (rs.next())
      {
        // int id = rs.getInt("ID");
        String username = rs.getString("username");
        String password = rs.getString("password");
        
        // print the results
        System.out.format("%s, %s, %s\n", id, username, password);
      }
    }

    /*
    *  given username or password, check the row data in table Users  
    */
    private void selectUsers(String s) throws SQLException {
      String query = "SELECT * FROM Users WHERE username='"+s+"'' OR password='"+s+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      
      while (rs.next())
      {
        int id = rs.getInt("ID");
        String username = rs.getString("username");
        String password = rs.getString("password");
        
        System.out.format("%s, %s, %s\n", id, username, password);
      }
    }

    /*
    *  given id, check the row data in table Credentials  
    */
    private void selectCredential(int id) throws SQLException {
      String query = "SELECT * FROM Credentials WHERE userid="+id;
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);

      while (rs.next())
      {
        // int id = rs.getInt("ID");
        String tag = rs.getString("tag");
        String data = rs.getString("data");

        System.out.format("%s, %s, %s\n", id, tag, data);
      }
    }

    /*
    *  given username or password, check the row data in table Credentials  
    */
    private void selectCredential(String s) throws SQLException {
      String query = "SELECT * FROM Users WHERE tag='"+s+"'' OR data='"+s+"'";
      st = c.createStatement();
      ResultSet rs = st.executeQuery(query);
      
      while (rs.next())
      {
        int id = rs.getInt("ID");
        String tag = rs.getString("tag");
        String data = rs.getString("data");
        
        System.out.format("%s, %s, %s\n", id, tag, data);
      }
    }

      // Constructor for testing purpose
  // public DatabaseManager() {
  //   try {
  //     connection();
  //     // put action HERE!
  //     // System.out.println(loginCheck("Lars", "Monsen"));
  //     // selectUsers(1);
  //     // insertData(1, "Facebook", "qwenjklsvnklsda");
  //     // editData(1, "Facebook", "I have a pen. I have an apple.");
  //     // register("Sue", "passsss");
      
  //     // storeData(1, "Facebook", "I don't care");// should export error 
  //     // storeData(1, "Facebook2", "I do care");// store the data 
  //     // storeData(loginCheck(Sue passsss), "Facebook", "Sue 234325");// store the data  
  //     // System.out.println(retrieve(1, "Facebook"));
  //     // System.out.println(retrieve(1, "Facebook3"));//error
  //     // System.out.println(retrieve(2, "Facebook"));

  //     end();
  //   } 
  //   catch (ClassNotFoundException e) {
  //     System.err.println("Got an exception! ClassNotFoundException");
  //     System.err.println(e.getMessage());
  //   } 
  //   catch (SQLException e) {
  //     System.err.println("Got an exception! SQLException");
  //     System.err.println(e.getMessage());
  //   }
  // }
  
  // public static void main(String[] args)
  // {
  //     new DatabaseManager();
  // }

}
