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

package org.github.ricall.junit5.sftp.api;

import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.github.ricall.junit5.sftp.implementation.DefaultSftpServerConfiguration;

import java.nio.file.Path;
import java.util.List;

/**
 * Used to configure the embedded sftp server.
 */
public interface ServerConfiguration {

    String DEFAULT_USERNAME = "username";
    String DEFAULT_PASSWORD = "password";

    /**
     * Used to create the configuration builder.
     *
     * @return A configuration builder
     */
    static ServerConfiguration configuration() {
        return DefaultSftpServerConfiguration.configuration();
    }

    /**
     * Set the port to use for the embedded sftp server.
     *
     * @param port The port number (between 1 and 65535)
     * @return The configuration builder
     */
    ServerConfiguration withPort(int port);

    /**
     * Add a username/password to the list of authorized users.
     * <p>
     * A sftp server can have multiple users, so this method can be called multiple times.
     * </p>
     * @param username The login username of the user
     * @param password The login password of the user
     * @return The configuration builder
     */
    ServerConfiguration withUser(String username, String password);

    /**
     * Add a list of resources to copy into the embedded sftp server for every test.
     * <p>
     * This method can be called multiple times so that any number of resources are copied.
     * The filesystem is wiped after each test and the files are recopied so that one test
     * will not impact another test
     * </p>
     * @param resources A list of all the resources to copy
     * @return The configuration builder
     */
    ServerConfiguration withResources(List<FileSystemResource> resources);

    /**
     * Add a list of authorized pub keys using a classpath resource.
     * <p>
     * The public keys in this classpath resource are used to authenticate against the clients private key
     * </p>
     * @param classpathResource The path to the resource containing the public keys
     * @return The configuration builder
     */
    ServerConfiguration withAuthorizedKeys(String classpathResource);

    /**
     * Add a list of authorized pub keys using a {@link Path}.
     * <p>
     * The public keys in this file are used to authenticate against the clients private key
     * </p>
     * @param withAuthorizedKeys The file containing the public keys
     * @return The configuration builder
     */
    ServerConfiguration withAuthorizedKeys(Path withAuthorizedKeys);

    /**
     * Provides a {@link KeyPairProvider} for host identity checking.
     *
     * @param keyPairProvider The {@link KeyPairProvider} containing the Hosts private key and the clients public key
     * @return The configuration builder
     */
    ServerConfiguration withKeyPairProvider(KeyPairProvider keyPairProvider);

}
