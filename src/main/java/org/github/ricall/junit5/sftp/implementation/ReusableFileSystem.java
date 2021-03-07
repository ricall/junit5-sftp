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

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.util.MockPath;
import org.apache.sshd.common.session.SessionContext;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public final class ReusableFileSystem extends FileSystem {

    private transient final FileSystem delegate;

    public ReusableFileSystem(final FileSystem fileSystem) {
        this.delegate = fileSystem;
    }

    @Override
    public FileSystemProvider provider() {
        return delegate.provider();
    }

    @Override
    public void close() {
        // Close is ignored allowing SftpSubsystemFactory to reuse the file system
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return delegate.getRootDirectories();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return delegate.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return delegate.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(final String first, final String... more) {
        return delegate.getPath(first, more);
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return delegate.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return delegate.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return delegate.newWatchService();
    }

    public static FileSystemFactory fileSystemFactory(final FileSystem fileSystem) {
        return new FileSystemFactory() {

            @Override
            public Path getUserHomeDir(final SessionContext session) {
                return new MockPath("/home/sftp");
            }

            @Override
            public FileSystem createFileSystem(final SessionContext session) {
                return new ReusableFileSystem(fileSystem);
            }

        };
    }

}
