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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.github.ricall.junit5.sftp.FileSystemResource.resourceAt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(SftpEmbeddedServerExtension.class)
public class TestSftpEmbeddedServerExtension {

    public final SftpServerConfiguration configuration = SftpServerConfiguration.configuration()
            .withPort(1234)
            .withUser("u1", "p1")
            .withUser("u2", "p2")
            .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"));

    private SftpClient getSftpClient() throws JSchException {
        return SftpClient.builder()
                .connectAs("u1", "p1")
                .port(1234)
                .build();
    }

    @Test
    public void verifyWeCanWriteTextFile(final SftpEmbeddedServer server) throws Exception {
        final SftpClient client = getSftpClient();

        final Path path = server.pathFor("sample file.txt");
        assertThat(Files.exists(path), is(false));
        client.writeFile("sample file.txt", "Sample file contents");
        assertThat(Files.exists(path), is(true));

        assertThat(client.readFile("sample file.txt"), is("Sample file contents"));
    }

    @Test
    public void verifyWeCanAddResourcesToSftp(final SftpEmbeddedServer server) throws Exception {
        server.addResources(resourceAt("/tmp/data/file3.txt").withText("file 3 contents"));

        // Access the sftp filesystem using the server
        final Path path = server.pathFor("/tmp/data/file1.txt");
        assertThat(Files.exists(path), is(true));
        assertThat(Files.lines(path).collect(Collectors.joining()), is("file 1 contents"));
        assertThat(Files.exists(server.pathFor("/tmp/data/file2.txt")), is(true));
        assertThat(Files.exists(server.pathFor("/tmp/data/file3.txt")), is(true));

        // Access the filesystem using the client
        final SftpClient client = getSftpClient();
        assertThat(client.readFile("/tmp/data/file1.txt"), is("file 1 contents"));
        assertThat(client.readFile("/tmp/data/file2.txt"), is("file 2 contents"));
        assertThat(client.readFile("/tmp/data/file3.txt"), is("file 3 contents"));
    }

}
