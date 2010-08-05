echo Building m2eclipse-wtp (no tests run)
set MAVEN_OPTS=-Xmx512m
mvn install -Dorg.maven.ide.eclipse.wtp.tests.skip=true 