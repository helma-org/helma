To get the manage application to work you need to

- add it to the `apps.properties` file with the following line:

```
manage
````

- use a Helma distribution version 1.5 or higher.

- add the following properties to the `server.properties` file:
 
```
allowAdmin = [comma-separated list of hosts or ip-addresses that
              are allowed to access this application. wildcards
              are only allowed in addresses, not in hostnames!]
adminAccess = <MD5-encoded credentials>
```

Creating the credentials can be done after you've got the application
up and running at this address:

```
http://<your-server-name>/manage/makekey
```

