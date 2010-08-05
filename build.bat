echo Building m2eclipse-wtp
set MAVEN_OPTS=-Xmx512m
mvn clean install -Dorg.maven.ide.eclipse.wtp.tests.skip=false 