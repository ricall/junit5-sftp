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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.sshd.common.file.util.MockPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.github.ricall.junit5.sftp.implementation.ReusableFileSystem.fileSystemFactory;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
class ReusableFileSystemTest {

    @Mock
    private FileSystem delegate;

    @InjectMocks
    private ReusableFileSystem fileSystem;

    @AfterEach
    public void cleanup() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void verifyProviderDelegatedAsExpected() {
        final FileSystemProvider provider = mock(FileSystemProvider.class);
        when(delegate.provider()).thenReturn(provider);

        assertThat(fileSystem.provider()).isSameAs(provider);

        verify(delegate).provider();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void verifyCloseIsNotDelegated() {
        fileSystem.close();
    }

    @Test
    public void verifyIsOpenIsDelegatedAsExpected() {
        when(delegate.isOpen()).thenReturn(true);

        assertThat(fileSystem.isOpen()).isEqualTo(true);

        verify(delegate).isOpen();
    }

    @Test
    public void verifyIsReadOnlyIsDelegatedAsExpected() {
        when(delegate.isReadOnly()).thenReturn(true);

        assertThat(fileSystem.isReadOnly()).isEqualTo(true);

        verify(delegate).isReadOnly();
    }

    @Test
    public void verifyGetSeparatorIsDelegatedAsExpected() {
        when(delegate.getSeparator()).thenReturn("/");

        assertThat(fileSystem.getSeparator()).isEqualTo("/");

        verify(delegate).getSeparator();
    }

    @Test
    public void verifyGetRootDirectoriesIsDelegatedAsExpected() {
        final Path path = mock(Path.class);
        when(delegate.getRootDirectories()).thenReturn(singletonList(path));

        assertThat(fileSystem.getRootDirectories()).containsExactly(path);

        verify(delegate).getRootDirectories();
    }

    @Test
    public void verifyGetFileStoresIsDelegatedAsExpected() {
        final FileStore fileStore = mock(FileStore.class);
        when(delegate.getFileStores()).thenReturn(singletonList(fileStore));

        assertThat(fileSystem.getFileStores()).containsExactly(fileStore);

        verify(delegate).getFileStores();
    }

    @Test
    public void verifySupportedFileAttributesViewIsDelegatedAsExpected() {
        when(delegate.supportedFileAttributeViews()).thenReturn(singleton("A"));

        assertThat(fileSystem.supportedFileAttributeViews()).containsExactly("A");

        verify(delegate).supportedFileAttributeViews();
    }

    @Test
    public void verifyGetPathIsDelegatedAsExpected() {
        final String[] more = {"second"};
        final Path path = mock(Path.class);
        when(delegate.getPath("first", more)).thenReturn(path);

        assertThat(fileSystem.getPath("first", more)).isSameAs(path);

        verify(delegate).getPath("first", more);
    }

    @Test
    public void verifyGetPathMatcherDelegatesAsExpected() {
        final PathMatcher pathMatcher = mock(PathMatcher.class);
        when(delegate.getPathMatcher("pattern")).thenReturn(pathMatcher);

        assertThat(fileSystem.getPathMatcher("pattern")).isSameAs(pathMatcher);

        verify(delegate).getPathMatcher("pattern");
    }

    @Test
    public void verifyGetUserPrincipalLookupServiceDelegatesAsExpected() {
        final UserPrincipalLookupService service = mock(UserPrincipalLookupService.class);
        when(delegate.getUserPrincipalLookupService()).thenReturn(service);

        assertThat(fileSystem.getUserPrincipalLookupService()).isSameAs(service);

        verify(delegate).getUserPrincipalLookupService();
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    public void verifyNewWatchServiceDelegatesAsExpected() throws IOException {
        final WatchService service = mock(WatchService.class);
        when(delegate.newWatchService()).thenReturn(service);

        assertThat(fileSystem.newWatchService()).isSameAs(service);

        verify(delegate).newWatchService();
    }

    @Test
    public void verifyFileSystemFactoryGetUserHomeDir() throws IOException {
        final Path path = fileSystemFactory(delegate).getUserHomeDir(null);

        assertThat(path).isInstanceOf(MockPath.class);
        assertThat(path.getFileName().toString()).isEqualTo("/home/sftp");
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    public void verifyFileSystemFactoryCreateFileSystem() throws IOException {
        final FileSystem newFileSystem = fileSystemFactory(delegate).createFileSystem(null);

        assertThat(newFileSystem).isInstanceOf(ReusableFileSystem.class);
    }

}
