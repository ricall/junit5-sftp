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
import org.github.ricall.junit5.sftp.api.EmbeddedSftpServer;
import org.github.ricall.junit5.sftp.api.ServerConfiguration;
import org.github.ricall.junit5.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.github.ricall.junit5.sftp.api.FileSystemResource.resourceAt;

@ExtendWith(EmbeddedSftpServerExtension.class)
public class TestEmbeddedSftpServer {

    public final ServerConfiguration configuration = ServerConfiguration.configuration()
            .withPort(1234)
            .withUser("user", "pass")
            .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"));

    private SftpClient getSftpClient() throws JSchException {
        return SftpClient.builder()
                .connectAs("user", "pass")
                .port(1234)
                .build();
    }

    @Test
    public void verifyClientCanWriteAsSimpleTextFile(final EmbeddedSftpServer server) throws Exception {
        try (SftpClient client = getSftpClient()) {
            final Path path = server.pathFor("sample file.txt");
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
    public void verifyAddResourceWorksAsExpected(final EmbeddedSftpServer server) throws Exception {
        // Add some addition resources
        rangeClosed(3, 10).boxed()
                .map(index -> resourceAt(filenameFor(index)).withText(bodyFor(index)))
                .forEach(server::addResources);

        // Access the sftp filesystem using the server
        for (final int index: rangeClosed(1, 10).toArray()) {
            final Path path = server.pathFor(filenameFor(index));

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
    public void verifyResetFilesystemWorksAsExpected(final EmbeddedSftpServer server) {
        server.addResources(resourceAt("/tmp/file.txt").withText("dummy file"));

        assertThat(Files.exists(server.pathFor("/tmp/file.txt"))).isTrue();
        assertThat(Files.isDirectory(server.pathFor("/home/sftp"))).isTrue();

        server.resetFileSystem();

        assertThat(Files.exists(server.pathFor("/tmp/file.txt"))).isFalse();
        assertThat(Files.isDirectory(server.pathFor("/home/sftp"))).isTrue();
    }

}
