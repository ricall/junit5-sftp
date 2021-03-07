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

import org.github.ricall.junit5.sftp.api.FileSystemResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultFileSystemResourceBuilderTest {

    private final DefaultFileSystemResourceBuilder builder = new DefaultFileSystemResourceBuilder("/tmp");

    @Mock
    private Path path;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private FileSystemProvider provider;

    @Mock
    private BasicFileAttributes attributes;

    @Test
    public void verifyUnknownClasspathResourcesThrowAnException() {
        assertThatExceptionOfType(ServerException.class)
            .isThrownBy(() -> builder.fromClasspathResource("/unknown.txt"))
            .withMessage("Unable to find classpath resource /unknown.txt");
    }

    @Test
    public void verifyUnlistableDirectoryThrowsAnException() throws Exception {
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(provider);

        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attributes);
        when(attributes.isDirectory()).thenReturn(true);
        when(provider.newDirectoryStream(eq(path), any())).thenThrow(new IOException("list failed"));

        assertThatExceptionOfType(ServerException.class)
                .isThrownBy(() -> builder.fromPath(path))
                .withMessageStartingWith("Failed to list directory");
    }

    @Test
    public void verifyUnreadableFileThrowsAnException() throws Exception {
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(provider);

        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attributes);
        when(attributes.isDirectory()).thenReturn(false);
        when(provider.newInputStream(path)).thenThrow(new IOException("read failed"));

        final List<FileSystemResource> resources = builder.fromPath(path);
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).toString()).isEqualTo("FileSystemResource(/tmp)");
        assertThatExceptionOfType(ServerException.class)
                .isThrownBy(() -> resources.get(0).getInputStream())
                .withMessageStartingWith("Failed to read");
    }

}
