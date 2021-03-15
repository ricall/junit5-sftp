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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.github.ricall.junit5.sftp.api.EmbeddedSftpServer;
import org.github.ricall.junit5.sftp.api.ServerConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.github.ricall.junit5.sftp.api.FileSystemResource.resourceAt;

@ExtendWith(EmbeddedSftpServerExtension.class)
public class TestEmbeddedSftpServerWithCommonsVfs {

    public final ServerConfiguration configuration = ServerConfiguration.configuration()
            .withPort(1234)
            .withUser("user", "pass")
            .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"));

    private static final StandardFileSystemManager FSM = new StandardFileSystemManager();

    @BeforeAll
    public static void init() throws FileSystemException {
        FSM.init();
    }

    @Test
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public void downloadFileInHomeFolder(final EmbeddedSftpServer server) throws IOException {
        server.addResources(resourceAt("file1.txt").withText("file 1 contents"));
        final SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();

        final FileSystemOptions options = new FileSystemOptions();
        builder.setStrictHostKeyChecking(options, "no");
        builder.setUserDirIsRoot(options, true);
        builder.setConnectTimeout(options, Duration.ofMillis(10_000));

        try (FileObject remoteFile = FSM.resolveFile("sftp://user:pass@localhost:1234/file1.txt", options)) {
            final String text = remoteFile.getContent().getString(StandardCharsets.UTF_8);
            assertThat(text).isEqualTo("file 1 contents");
        }
    }

    @Test
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public void downloadFileUsingAbsolutePath(final EmbeddedSftpServer server) throws IOException {
        server.addResources(resourceAt("/tmp/file1.txt").withText("file in /tmp folder"));
        final SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();

        final FileSystemOptions options = new FileSystemOptions();
        builder.setStrictHostKeyChecking(options, "no");
        builder.setUserDirIsRoot(options, false);
        builder.setConnectTimeout(options, Duration.ofMillis(10_000));

        try (FileObject remoteFile = FSM.resolveFile("sftp://user:pass@localhost:1234/tmp/file1.txt", options)) {
            final String text = remoteFile.getContent().getString(StandardCharsets.UTF_8);
            assertThat(text).isEqualTo("file in /tmp folder");
        }
    }

}
