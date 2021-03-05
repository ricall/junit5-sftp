# JUnit 5 SFTP extension

## What is it?

This JUnit 5 extension that provides a way to run a SFTP server in your JUnit code.

## How to use it

### Gradle
Add the dependency to gradle
```groovy
testImplementation 'io.github.ricall.junit5-sftp:junit5-sftp:1.0.0'
```

### Maven
Add the dependeny to mvn pom.xml
```xml
<dependency>
    <groupId>io.github.ricall.junit5-sftp</groupId>
    <artifactId>junit5-sftp</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### Using the `JUnit5` extension
```java
@ExtendWith(SftpEmbeddedServerExtension.class)
public class TestSftpEmbeddedServer {

    public final SftpServerConfiguration configuration = SftpServerConfiguration.configuration()
            .withPort(1234)
            .withUser("username", "password")
            .withResources("/tmp/data", "data");

    @Test
    public void verifyWeCanWriteTextFile(final SftpEmbeddedServer server) {
        // sftp server is running on sftp://username:password@localhost:1234
    }
    
}
```
## License
This software is licensed using [MIT](https://opensource.org/licenses/MIT) 