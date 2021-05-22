# JUnit 5 SFTP server

## What is it?

This JUnit 5 libary provides a way to create a sftp server that can be used for integration testing.

## How to use it

### Gradle

Add the dependency to gradle

```groovy
testImplementation 'io.github.ricall.junit5-sftp:junit5-sftp:2.0.0'
```

### Maven

Add the dependency to mvn pom.xml

```xml

<dependency>
    <groupId>io.github.ricall.junit5-sftp</groupId>
    <artifactId>junit5-sftp</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

## Using the `JUnit5` extension

```java
public class TestEmbeddedSftpServer {

    @RegisterExtension
    public final EmbeddedSftpServer sftpServer = SftpServer.defaultSftpServer()
            .withPort(3022)
            .withUser("user", "pass")
            .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"))
            .build();

    @Test
    public void verifySftpFilesCanBeDownloaded() {
        // sftp server is running on sftp://user:pass@localhost:3022
        // the files and directories from the data package are copied into the /tmp/data folder
        // on the sftp server
    }
    
}
```

The FTP server uses an in-memory FileSystem to manage files, between tests the file system is recreated so that tests do
not impact each other.

## Using the `FileSystemResource` abstraction.

`@SftpEmbeddableServerExtension` provides a powerful `FileSystemResource` abstraction that allows you to populate the
in-memory FileSystem with files/folders in a simple easy to use fashion.

---

### FileSystemResource `withText`

Return a `FileSystemResource` that will populate the `/tmp/file1.txt` file with `File Contents`

```java
FileSystemResource.resourceAt("/tmp/file1.txt").withText("File Contents")
```

---

### FileSystemResource `withContent`

Return a `FileSystemResource` that will populate the `/tmp/file2.txt` file with the data returned from
createFileInputStream()

```java
FileSystemResource.resourceAt("/tmp/file2.txt").withContent(this::createFileInputStream)
```

---

### FileSystemResource `fromClasspathResource`

Return a list of `FileSystemResource`'s that will populate the `/tmp/test-data` folder with all files/directories under
the `test.data` package.

```java
FileSystemResource.resourceAt("/tmp/test-data").fromClasspathResource("/test/data")
```

---

### FileSystemResource `fromPath`

Return a list of `FileSystemResource`'s that will populate the `/tmp/folder` folder with all the files/directories
under `path` (path can be files/folders on your hard drive, files/folders in a ZIP/TAR container or event files/folders
in another in-memory filesystem)

```java
FileSystemResource.resourceAt("/tmp/folder").fromPath(path)
```

## License

This software is licensed using [MIT](https://opensource.org/licenses/MIT) 