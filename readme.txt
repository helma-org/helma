To get the manage-application to work you must:

 - add it to the apps.properties file with the following line:

     manage

 - use snapshot 20020326 or later.

 - add the following properties to server.properties:
 
   allowAdmin=[comma-separated list of hosts or ip-addresses that
               are allowed to access this application, wildcards
               are only allowed in addresses, not in hostnames]
   adminUsername=<md5-encoded username>
   adminPassword=<md5-encoded password>
   
   MD5-encoding can be done - once you've got the application
   up and running - on the web:

     http://<your-server-name>/manage/makekey
     http://<your-server-name>/base/makekey

   or you can use a shell-tool integrated in helma:
     java -classpath helma.jar helma.util.MD5Encoder <username> <password>

