To get the manage-application to work you must:

 - add it to the apps.properties file with the following line:
     manage

 - use helma distribution 1.2-RC1 or later.

 - add the following properties to server.properties:
 
   allowAdmin = [comma-separated list of hosts or ip-addresses that
                 are allowed to access this application. wildcards
                 are only allowed in addresses, not in hostnames!]
   adminAccess = <MD5-encoded credentials>
   
   Creating the credentials can be done after you've got the application
   up and running at this address: http://<your-server-name>/manage/makekey

