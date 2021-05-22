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

package org.github.ricall.junit5.sftp.implementation;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.github.marschall.memoryfilesystem.StringTransformers;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.github.ricall.junit5.sftp.EmbeddedSftpServer;
import org.github.ricall.junit5.sftp.FileSystemResource;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.github.ricall.junit5.sftp.SftpServer.DEFAULT_PASSWORD;
import static org.github.ricall.junit5.sftp.SftpServer.DEFAULT_USERNAME;

public final class DefaultEmbeddedSftpServer implements EmbeddedSftpServer {

    private static final String PATH_SEPARATOR = "/";
    private static final String SFTP_USER_AND_GROUP = "sftp";
    private static final String HOME_DIRECTORY = "/home/sftp";

    private transient final SftpConfiguration configuration;
    private transient FileSystem fileSystem;
    private transient SshServer server;

    public DefaultEmbeddedSftpServer(final SftpConfiguration configuration) {
        this.configuration = configuration;

        if (configuration.noAuthenticationDefined()) {
            configuration.withUser(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    public void startServer() {
        fileSystem = createFileSystem();

        final SshServer newServer = SshServer.setUpDefaultServer();
        newServer.setPort(configuration.getPort());
        newServer.setPasswordAuthenticator(configuration);
        if (configuration.getAuthorizedKeys() != null) {
            newServer.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(configuration.getAuthorizedKeys()));
        }
        newServer.setKeyPairProvider(configuration.getKeyPairProvider());
        newServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        newServer.setFileSystemFactory(ReusableFileSystem.fileSystemFactory(fileSystem));
        addResources(configuration.getResources());

        try {
            newServer.start();
        } catch (IOException e) {
            throw new ServerException("Failed to start the SFTP serverInstance", e);
        }
        this.server = newServer;
    }

    private FileSystem createFileSystem() {
        try {
            return MemoryFileSystemBuilder.newEmpty()
                    .addRoot(PATH_SEPARATOR)
                    .setSeparator(PATH_SEPARATOR)
                    .addUser(SFTP_USER_AND_GROUP)
                    .addGroup(SFTP_USER_AND_GROUP)
                    .addFileAttributeView(PosixFileAttributeView.class)
                    .setCurrentWorkingDirectory(HOME_DIRECTORY)
                    .setStoreTransformer(StringTransformers.IDENTIY)
                    .setCaseSensitive(true)
                    .setSupportFileChannelOnDirectory(true)
                    .addForbiddenCharacter((char) 0)
                    .build("sftpFileSystem." + UUID.randomUUID());
        } catch (IOException e) {
            throw new ServerException("Failed to create FileSystem", e);
        }
    }

    @Override
    public void addResources(final List<FileSystemResource> resources) {
        resources.forEach(resource -> {
            final Path destination = fileSystem.getPath(resource.getDestination());
            ensurePathExists(destination);

            try {
                Files.copy(resource.getInputStream(), destination);
            } catch (IOException e) {
                throw new ServerException("Failed to copy " + resource + " to FileSystem", e);
            }
        });
    }

    private void ensurePathExists(final Path path) {
        try {
            final Path parent = path.getParent();
            if (parent != null && !parent.equals(path.getRoot())) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ServerException("Failed to create folder " + path, e);
        }
    }

    @Override
    public void resetFileSystem() {
        try {
            fileSystem.close();
            fileSystem = createFileSystem();
            server.setFileSystemFactory(ReusableFileSystem.fileSystemFactory(fileSystem));
            addResources(configuration.getResources());
        } catch (IOException e) {
            throw new ServerException("Failed to close FileSystem", e);
        }
    }

    @Override
    public Path pathFor(final String filename, final String... more) {
        return fileSystem.getPath(filename, more);
    }

    public void stopServer() {
        try {
            server.stop(false);
        } catch (IOException e) {
            throw new ServerException("Failed to stop SFTP server", e);
        }
    }

}
