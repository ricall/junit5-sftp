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

import org.github.ricall.junit5.sftp.FileSystemResource;
import org.github.ricall.junit5.sftp.FileSystemResource.FileSystemResourceBuilder;
import org.github.ricall.junit5.sftp.SftpServerException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class DefaultFileSystemResourceBuilder implements FileSystemResourceBuilder {

    private final transient String destination;

    public DefaultFileSystemResourceBuilder(final String destination) {
        this.destination = destination;
    }

    private DefaultFileSystemResourceBuilder(final DefaultFileSystemResourceBuilder parent, final String name) {
        this.destination = String.format("%s/%s", parent.destination, name);
    }

    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public List<FileSystemResource> withText(final String text) {
        final byte[] bytes = text.getBytes(Charset.defaultCharset());
        return withContent(() -> new ByteArrayInputStream(bytes));
    }

    @Override
    public List<FileSystemResource> withContent(final Supplier<InputStream> streamSupplier) {
        return singletonList(new SimpleFileSystemResource(destination, streamSupplier));
    }

    @Override
    public List<FileSystemResource> fromClasspathResource(final String classpathResource) {
        try {
            final URL resource = getClass().getResource(classpathResource);
            if (resource == null) {
                throw new SftpServerException("Failed to find classpath resource " + classpathResource);
            }
            return fromPath(Paths.get(resource.toURI()));
        } catch (URISyntaxException use) {
            throw new SftpServerException("Failed to resolve " + classpathResource, use);
        }
    }

    @Override
    public List<FileSystemResource> fromPath(final Path path) {
        return collectPathResources(path, true);
    }

    private List<FileSystemResource> recurseFromPath(final Path path) {
        return collectPathResources(path, false);
    }

    private List<FileSystemResource> collectPathResources(final Path path, final boolean isTopLevel) {
        final DefaultFileSystemResourceBuilder builder = builderFor(path, isTopLevel);
        if (Files.isDirectory(path)) {
            try {
                return Files.list(path)
                        .map(builder::recurseFromPath)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new SftpServerException("Failed to list directory " + path, e);
            }
        } else {
            return builder.withContent(asResource(path));
        }
    }

    private DefaultFileSystemResourceBuilder builderFor(final Path path, final boolean isTopLevel) {
        if (isTopLevel) {
            return this;
        }
        return new DefaultFileSystemResourceBuilder(this, Optional.ofNullable(path)
                .map(Path::getFileName)
                .map(Object::toString)
                .orElse("UNKNOWN FILE"));
    }

    private Supplier<InputStream> asResource(final Path path) {
        return () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new SftpServerException("Failed to read " + path, e);
            }
        };
    }

    static final public class SimpleFileSystemResource implements FileSystemResource {

        private final String destination;
        private final transient Supplier<InputStream> resource;

        public SimpleFileSystemResource(final String destination, final Supplier<InputStream> resource) {
            this.destination = destination;
            this.resource = resource;
        }

        @Override
        public String getDestination() {
            return destination;
        }

        @Override
        public InputStream getInputStream() {
            return resource.get();
        }

        @Override
        public String toString() {
            return String.format("FileSystemResource(%s)", destination);
        }

    }

}
