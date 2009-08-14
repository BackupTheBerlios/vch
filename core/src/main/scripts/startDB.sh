MAVEN_REPOSITORY=~/daten/maven/repository
java -cp $MAVEN_REPOSITORY/hsqldb/hsqldb/1.8.0.1/hsqldb-1.8.0.1.jar org.hsqldb.Server -database.0 file:../../../data/hsqldb -dbname.0 VODCH
