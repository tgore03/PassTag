// import databasepkg.DatabaseManager;


public class _testDatabaseManager
{

	public _testDatabaseManager() {
	    try {
    		DatabaseManager manager = new DatabaseManager();
			manager.connection();
			// put action HERE!
			System.out.println(manager.loginCheck("Lars", "Monsen"));
			manager.selectUsers(1);
			// manager.insertData(1, "Facebook", "qwenjklsvnklsda");
			manager.editData(1, "Facebook", "I have a pen. I have an apple.");
			manager.register("Sue", "passsss");

			manager.storeData(1, "Facebook", "I don't care");// should export error 
			manager.storeData(1, "Facebook2", "I do care");// store the data 
			manager.storeData(manager.loginCheck(Sue passsss), "Facebook", "Sue 234325");// store the data  
			System.out.println(manager.retrieve(1, "Facebook"));
			System.out.println(manager.retrieve(1, "Facebook3"));//error
			System.out.println(manager.retrieve(2, "Facebook"));

			manager.end();
	    } 
	    catch (ClassNotFoundException e) {
	      System.err.println("Got an exception! ClassNotFoundException");
	      System.err.println(e.getMessage());
	    } 
	    catch (SQLException e) {
	      System.err.println("Got an exception! SQLException");
	      System.err.println(e.getMessage());
	    }
	}

	public static void main(String[] args) {
	      new _testDatabaseManager();
	}
}