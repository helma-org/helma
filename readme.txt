To get the manage-application to work you must:

 - add it to the apps.properties file with the following line

     manage=self

   instead of just the application name. This is to let the 
   application manager know that the subject of the application
   is the server itself.

 - use snapshot 200203xx or later.

 - add the following properties to server.properties:
 
   allowAdmin=[comma-separated list of hosts or ip-addresses that
               are allowed to access this application, wildcards
               are only allowed in addresses, not in hostnames]
   adminUsername=<md5-encoded username>
   adminPassword=<md5-encoded password>
   
   MD5-encoding can be done either on the shell:
     java -classpath helma.jar helma.util.MD5Encode <username> <password>

   Or - once you've got the application up and running - on the web:
     http://<your-server-name>/manage/makekey
     http://<your-server-name>/base/makekey

