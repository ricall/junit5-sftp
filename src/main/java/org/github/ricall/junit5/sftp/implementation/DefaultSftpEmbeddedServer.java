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
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.github.ricall.junit5.sftp.FileSystemResource;
import org.github.ricall.junit5.sftp.SftpEmbeddedServer;
import org.github.ricall.junit5.sftp.SftpServerException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.*;

import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_PASSWORD;
import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_USERNAME;

public final class DefaultSftpEmbeddedServer implements SftpEmbeddedServer {

    private static final String PATH_SEPARATOR = "/";
    private static final String SFTP_USER_AND_GROUP = "sftp";
    private static final String HOME_DIRECTORY = "/home/sftp";

    private transient final DefaultSftpServerConfiguration configuration;
    private transient FileSystem fileSystem;
    private transient SshServer server;

    public DefaultSftpEmbeddedServer(final DefaultSftpServerConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.getUsers().isEmpty()) {
            configuration.getUsers().put(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        }
    }

    public void startServer() {
        fileSystem = createFileSystem();

        this.server = SshServer.setUpDefaultServer();
        server.setPort(configuration.getPort());
        server.setKeyPairProvider(configuration.getKeyPairProvider());
        server.setPasswordAuthenticator(configuration);
        server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        server.setFileSystemFactory(ReusableFileSystem.fileSystemFactory(fileSystem));
        addResources(configuration.getResources());

        try {
            server.start();
        } catch (IOException e) {
            throw new SftpServerException("Failed to start the SFTP serverInstance", e);
        }
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
            throw new SftpServerException("Failed to create FileSystem", e);
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
                throw new SftpServerException("Failed to copy " + resource + " to FileSystem", e);
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
            throw new SftpServerException("Failed to create folder " + path, e);
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
            throw new SftpServerException("Failed to close FileSystem", e);
        }
    }

    @Override
    public Path pathFor(final String filename, final String... more) {
        return fileSystem.getPath(filename, more);
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (IOException e) {
            throw new SftpServerException("Failed to stop SFTP server", e);
        }
    }

}
