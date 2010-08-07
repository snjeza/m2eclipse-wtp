echo Building m2eclipse-wtp using Eclipse 3.5 target platform
set MAVEN_OPTS=-Xmx512m
mvn clean install -Dorg.maven.ide.eclipse.wtp.tests.skip=false -Dtarget.platform=m2e-wtp-e35