This is the first release of Helma Object Publisher. 

If you manage to get it running you should be able to connect your browser to 
http://127.0.0.1:8080/ (port 8080, that is). There is not much in terms of documentation 
at this point, please look at http://helma.org and have a look at the sample application 
in the apps/hopblog directory.

This version is set up to use its own embedded Web server and a very basic 
embedded object database. For this reason it is able to run virtually without installation 
on any platform with a Java 1.1 virtual machine. 

On the other hand, the embedded Web server and object db are meant for 
development work and not ready for prime time deployment. For that you'd probably 
use an external relational database, the Berkeley DB package and a full featured 
Web server like Apache.

Stay tuned for more documentation and stuff
hannes@helma.at
http://helma.org/
