/*
 * Copyright (c) 2021 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.github.ricall.junit5.sftp;

import com.jcraft.jsch.JSchException;
import org.github.ricall.junit5.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.github.ricall.junit5.sftp.FileSystemResource.resourceAt;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class TestEmbeddedSftpServerWithPerClassLifecycle {

    public static final String TEMP_FILE = "/tmp/file1.txt";

    @RegisterExtension
    public final EmbeddedSftpServer sftpServer = SftpServer.defaultSftpServer()
            .withPort(3022)
            .withUser("user", "pass")
            .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"))
            .build();

    private SftpClient getSftpClient() throws JSchException {
        return SftpClient.builder()
                .connectAs("user", "pass")
                .port(3022)
                .build();
    }

    @Test
    public void verifyWeCanWriteTheSameFileTwice() throws IOException {
        sftpServer.addResources(resourceAt(TEMP_FILE).withText("file1 contents"));

        assertThat(Files.readAllLines(sftpServer.pathFor(TEMP_FILE))).contains("file1 contents");
    }

    @Test
    public void verifyWeCanWriteTheSameFileTwiceRedux() throws IOException {
        sftpServer.addResources(resourceAt(TEMP_FILE).withText("file1 contents redux"));

        assertThat(Files.readAllLines(sftpServer.pathFor(TEMP_FILE))).contains("file1 contents redux");
    }

    @Test
    public void verifyClientCanWriteAsSimpleTextFile() throws Exception {
        try (SftpClient client = getSftpClient()) {
            final Path path = sftpServer.pathFor("sample file.txt");
            assertThat(Files.exists(path)).isEqualTo(false);

            client.writeFile("sample file.txt", "Sample file contents");

            assertThat(Files.exists(path)).isEqualTo(true);
            assertThat(client.readFile("sample file.txt")).isEqualTo("Sample file contents");
        }
    }

    private String filenameFor(final int index) {
        return format("/tmp/data/file%d.txt", index);
    }

    private String bodyFor(final int index) {
        return format("file %d contents", index);
    }

    @Test
    public void verifyAddResourceWorksAsExpected() throws Exception {
        // Add some additional resources
        rangeClosed(3, 10).boxed()
                .map(index -> resourceAt(filenameFor(index)).withText(bodyFor(index)))
                .forEach(sftpServer::addResources);

        // Access the sftp filesystem using the server
        for (final int index: rangeClosed(1, 10).toArray()) {
            final Path path = sftpServer.pathFor(filenameFor(index));

            assertThat(Files.exists(path)).isEqualTo(true);
            assertThat(Files.lines(path).collect(Collectors.joining())).isEqualTo(bodyFor(index));
        }

        // Access the filesystem using the client
        try (SftpClient client = getSftpClient()) {
            for (final int index: rangeClosed(1, 10).toArray()) {
                final String filename = filenameFor(index);
                final String expectedBody = bodyFor(index);

                assertThat(client.readFile(filename)).isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void verifyResetFilesystemWorksAsExpected() {
        sftpServer.addResources(resourceAt("/tmp/file.txt").withText("dummy file"));

        assertThat(Files.exists(sftpServer.pathFor("/tmp/file.txt"))).isTrue();
        assertThat(Files.isDirectory(sftpServer.pathFor("/home/sftp"))).isTrue();

        sftpServer.resetFileSystem();

        assertThat(Files.exists(sftpServer.pathFor("/tmp/file.txt"))).isFalse();
        assertThat(Files.isDirectory(sftpServer.pathFor("/home/sftp"))).isTrue();
    }

}
