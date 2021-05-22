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

import org.github.ricall.junit5.sftp.implementation.DefaultFileSystemResourceBuilder;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Provides a way to create a list resources that need to be coped into the embedded sftp filesystem.
 * <p>
 * You need to use the builder {@link FileSystemResource#resourceAt(String)} to create resources.
 * The builder provides a Domain Specific Language (DSL) for easily copying files/directories from
 * any source into the embedded sftp filesystem.
 * </p>
 */
public interface FileSystemResource {

    /**
     * Get the destination path of the file.
     *
     * @return The path to the file in the embedded sftp filesystem
     */
    String getDestination();

    /**
     * Get the input stream for the resource.
     *
     * @return The contents of the file that will be written to the filesystem
     */
    InputStream getInputStream();

    /**
     * Create a builder for a list of {@link FileSystemResource} objects.
     *
     * @param destination The path to the file/directory in the embedded sftp filesystem
     * @return The {@link FileSystemResourceBuilder} builder for creating resources
     */
    static FileSystemResourceBuilder resourceAt(String destination) {
        return new DefaultFileSystemResourceBuilder(destination);
    }

    /**
     * Domain Specific Language for creating resources in the embedded sftp filesystem.
     */
    interface FileSystemResourceBuilder {

        /**
         * Single file containing the provided text.
         *
         * @param text The text contents of the file
         * @return A list containing a single {@link FileSystemResource} for the file
         */
        List<FileSystemResource> withText(String text);

        /**
         * Single file containing data from the supplied input stream.
         * <p>
         * Because the embedded filesystem is recreated for every test we need to supply a new input stream
         * for each test.
         * </p>
         * @param streamSupplier Supplies the {@link InputStream} to populate the file
         * @return A list containing a single {@link FileSystemResource} for the file
         */
        List<FileSystemResource> withContent(Supplier<InputStream> streamSupplier);

        /**
         * Copy classpath resources into the embedded sftp filesystem.
         * <p>
         * The classpath resources can be a single file or a directory. If a directory is used then all
         * subfolders/files will be added to the list of {@link FileSystemResource}
         * </p>
         * @param classpathResource The path to the resource (Starting with /)
         * @return A list containing all the classpath resources
         */
        List<FileSystemResource> fromClasspathResource(String classpathResource);

        /**
         * Copy filesystem resources into the embedded sftp filesystem.
         * <p>
         * The path can represent a file or directory. If a directory is used then all subfolders/files will
         * be added to the list of {@link FileSystemResource}
         * </p>
         * @param path The path to the resource (either a file or directory)
         * @return A list containing all the filesystem resources
         */
        List<FileSystemResource> fromPath(Path path);

    }

}
