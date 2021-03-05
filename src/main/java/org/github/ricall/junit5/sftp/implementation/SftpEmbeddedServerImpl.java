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
import org.github.ricall.junit5.sftp.SftpEmbeddedServer;
import org.github.ricall.junit5.sftp.SftpServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.*;
import java.util.stream.Stream;

import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_PASSWORD;
import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_USERNAME;

public final class SftpEmbeddedServerImpl implements SftpEmbeddedServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpEmbeddedServerImpl.class);
    private static final String PATH_SEPARATOR = "/";
    private static final String SFTP_USER_AND_GROUP = "sftp";
    private static final String HOME_DIRECTORY = "/home/sftp";

    private transient final SftpMutableServerConfiguration configuration;
    private transient FileSystem fileSystem;
    private transient SshServer server;

    public SftpEmbeddedServerImpl(final SftpMutableServerConfiguration configuration) {
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
        prepareFileSystem();

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

    private void prepareFileSystem() {
        configuration.getResources().entrySet().forEach(entry -> addResources(entry.getKey(), entry.getValue()));
    }

    @Override
    public void addResources(final String path, final String... resources) {
        Arrays.stream(resources).forEach(resource -> {
            LOGGER.info("Add Resource {}/{}", path, resource);
            final Stream<Path> files = Optional.ofNullable(getClass().getResource(PATH_SEPARATOR + resource))
                    .map(URL::getPath)
                    .map(Paths::get)
                    .map(this::filesFrom)
                    .orElse(Stream.empty());

            files.forEach(child -> {
                try {
                    if (Files.isDirectory(child)) {
                        final String newPath = String.format("%s/%s", path, child.getFileName());
                        addResources(newPath, Files.list(child)
                                .map(Path::getFileName)
                                .map(Object::toString)
                                .toArray(String[]::new));
                    } else {
                        addFile(path, child);
                    }
                } catch (IOException e) {
                    throw new SftpServerException("Failed to add resources", e);
                }
            });
        });
    }

    public void addFile(final String path, final Path source) {
        final Path destinationPath = fileSystem.getPath(path, Optional.ofNullable(source)
            .map(Path::getFileName)
            .map(Object::toString)
            .orElse(""));
        try {
            ensurePathExists(destinationPath);
            Files.copy(Files.newInputStream(source), destinationPath);
        } catch (IOException e) {
            throw new SftpServerException("Failed to copy file " + source + " --> " + destinationPath, e);
        }
    }

    private void ensurePathExists(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null && !parent.equals(path.getRoot())) {
            Files.createDirectories(parent);
        }
    }

    @Override
    public void resetFileSystem() {
        try {
            fileSystem.close();
            fileSystem = createFileSystem();
            server.setFileSystemFactory(ReusableFileSystem.fileSystemFactory(fileSystem));
            prepareFileSystem();
        } catch (IOException e) {
            throw new SftpServerException("Failed to close FileSystem", e);
        }
    }

    @Override
    public Path pathFor(final String filename, final String... more) {
        return fileSystem.getPath(filename, more);
    }

    private Stream<Path> filesFrom(final Path path) {
        if (Files.isDirectory(path)) {
            try {
                return Files.list(path);
            } catch (IOException e) {
                LOGGER.error("failed to list files for {}", path, e);
                return Stream.empty();
            }
        }
        return Stream.of(path);
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (IOException e) {
            throw new SftpServerException("Failed to stop SFTP server", e);
        }
    }

}
