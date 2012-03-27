To run the tests, add something like this to your apps.properties file:

test
test.appdir = apps/test/code
test.repository.0 = apps/test/code
test.repository.1 = modules/jala/util/Test/code

And you need to have a JDBC driver for your database in lib/ext,
as well as create the database schema provided in one of the
db-<product>.sql files.
