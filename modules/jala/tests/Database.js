/**
 * Contains the system's temporary directory
 * @type helma.File
 * @private
 */
var tmpDir = new helma.File(java.lang.System.getProperty("java.io.tmpdir"));

/**
 * Contains the server created in testServer method
 * @private
 */
var server = null;

/**
 * Basic tests for jala.db.RamDatabase. All of these tests are
 * valid for jala.db.FileDatabase too.
 */
var testRamDatabase = function() {
   var db = new jala.db.RamDatabase("test");
   assertNotNull(db);
   assertEqual(db.getName(), "test");
   assertEqual(db.getDatabasePath(), "mem:test");
   assertEqual(db.getUrl(), "jdbc:h2:mem:test");
   // test connection to database
   var conn = db.getConnection();
   assertNotNull(conn);
   assertTrue(conn instanceof helma.Database);

   // create a table
   db.createTable("test", [
      {
         name: "id",
         type: java.sql.Types.INTEGER,
         nullable: false,
         unique: true
      },
      {
         name: "name",
         type: java.sql.Types.VARCHAR,
         length: 255
      }
   ], "id");

   // test if the table exists
   assertTrue(db.tableExists("test"));

   // dump database
   var dumpFile = new helma.File(tmpDir, "backup.test.sql");
   assertTrue(db.dump(dumpFile));
   assertTrue(dumpFile.exists());
   assertTrue(dumpFile.getLength() > 0);
   // remove dump file again
   dumpFile.remove();

   // drop table
   db.dropTable("test");
   assertFalse(db.tableExists("test"));

   // test db shutdown
   db.shutdown();
   assertThrows(function() {
      conn.query("select 1 = 1");
   }, Packages.org.h2.jdbc.JdbcSQLException);
   return;
};

/**
 * Basic tests for jala.db.FileDatabase that are different to
 * jala.db.RamDatabase
 */
var testFileDatabase = function() {
   var db = new jala.db.FileDatabase("test", tmpDir);
   assertNotNull(db);
   assertEqual(db.getName(), "test");
   assertEqual(db.getDirectory(), tmpDir);

   var dbDir = new helma.File(tmpDir, "test");
   assertEqual(db.getDatabasePath(), "file:" + dbDir.getAbsolutePath());
   assertEqual(db.getUrl(), "jdbc:h2:file:" + dbDir.getAbsolutePath());

   // execute sql script (need to do that, otherwise the backup won't
   // work because the database is empty)
   var sqlFile = jala.Test.getTestFile("Database.script.sql");
   assertTrue(db.runScript(sqlFile));
   assertTrue(db.tableExists("test"));

   // test backup
   var backupFile = new helma.File(tmpDir, "backup.zip");
   assertTrue(db.backup(backupFile));
   assertTrue(backupFile.exists());
   assertTrue(backupFile.getLength() > 0);

   // remove the database
   db.remove();
   assertFalse((new helma.File(db.getDirectory(), db.getName() + ".data.db")).exists());
   assertFalse((new helma.File(db.getDirectory(), db.getName() + ".index.db")).exists());
   assertFalse((new helma.File(db.getDirectory(), db.getName() + ".trace.db")).exists());

   // test restore
   assertTrue(db.restore(backupFile));
   assertTrue(db.tableExists("test"));

   // remove backup file and database
   backupFile.remove();
   db.remove();
   
   return;
};

var testServer = function() {
   server = new jala.db.Server(tmpDir);
   // test default config
   assertEqual(tmpDir, server.getDirectory());
   assertEqual(server.getPort(), 9092);
   assertFalse(server.useSsl());
   assertFalse(server.isPublic());
   assertFalse(server.createOnDemand());

   // test setting config properties
   server.useSsl(true);
   assertTrue(server.useSsl());
   server.isPublic(true);
   assertTrue(server.isPublic());
   server.createOnDemand(true);
   assertTrue(server.createOnDemand());

   // reset back some of them
   server.useSsl(false);
   assertFalse(server.useSsl());
   server.isPublic(false);
   assertFalse(server.isPublic());

   // start the server
   assertTrue(server.start());

   // test connection properties (this also includes testing
   // of server.getUrl())
   var props = server.getProperties("test", "test", "1111");
   assertEqual(props.getProperty("test.url"), "jdbc:h2:tcp://localhost:9092/test");
   assertEqual(props.getProperty("test.driver"), "org.h2.Driver");
   assertEqual(props.getProperty("test.user"), "test");
   assertEqual(props.getProperty("test.password"), "1111");

   var conn = server.getConnection("test", "test", "1111");
   assertNotNull(conn);

   // stop the server
   assertTrue(server.stop());
   // and remove the file database created above
   var db = new jala.db.FileDatabase("test", tmpDir, "test", "1111");
   db.remove();
   return;
};

/**
 * Stuff to do on cleanup
 */
var cleanup = function() {
   if (server != null) {
      server.stop();
   }
   return;
};
