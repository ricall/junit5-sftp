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

import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.github.ricall.junit5.sftp.implementation.SftpConfiguration;

import java.nio.file.Path;
import java.util.List;

/**
 * Builder used to create EmbeddedSftpServer instances for testing.
 * To use the builder:
 * <pre>{@code
 * public class TestEmbeddedSftpServer {
 *
 *     @RegisterExtension
 *     public final EmbeddedSftpServer sftpServer = SftpServer.defaultSftpServer()
 *             .withPort(3022)
 *             .withUser("user", "pass")
 *             .withResources(resourceAt("/tmp/data").fromClasspathResource("/data"))
 *             .build();
 *
 *     @Test
 *     public void verifySftpFilesCanBeDownloaded() {
 *         // sftp server is running on sftp://user:pass@localhost:3022
 *         // the files and directories from the data package are copied into the /tmp/data folder
 *         // on the sftp server
 *     }
 *
 * }
 * }</pre>>
 */
public interface SftpServer {

    String DEFAULT_USERNAME = "username";
    String DEFAULT_PASSWORD = "password";

    /**
     * Default builder for the embedded sftp server.
     *
     * @return A configuration builder
     */
    static SftpServer defaultSftpServer() {
        return SftpConfiguration.configuration();
    }

    /**
     * Set the port to use for the embedded sftp server.
     *
     * @param port The port number (between 1 and 65535)
     * @return The configuration builder
     */
    SftpServer withPort(int port);

    /**
     * Add a username/password to the list of authorized users.
     * <p>
     * A sftp server can have multiple users, so this method can be called multiple times.
     * </p>
     * @param username The login username of the user
     * @param password The login password of the user
     * @return The configuration builder
     */
    SftpServer withUser(String username, String password);

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
    SftpServer withResources(List<FileSystemResource> resources);

    /**
     * Add a list of authorized pub keys using a classpath resource.
     * <p>
     * The public keys in this classpath resource are used to authenticate against the clients private key
     * </p>
     * @param classpathResource The path to the resource containing the public keys
     * @return The configuration builder
     */
    SftpServer withAuthorizedKeys(String classpathResource);

    /**
     * Add a list of authorized pub keys using a {@link Path}.
     * <p>
     * The public keys in this file are used to authenticate against the clients private key
     * </p>
     * @param withAuthorizedKeys The file containing the public keys
     * @return The configuration builder
     */
    SftpServer withAuthorizedKeys(Path withAuthorizedKeys);

    /**
     * Provides a {@link KeyPairProvider} for host identity checking.
     *
     * @param keyPairProvider The {@link KeyPairProvider} containing the Hosts private key and the clients public key
     * @return The configuration builder
     */
    SftpServer withKeyPairProvider(KeyPairProvider keyPairProvider);

    /**
     * Create a EmbeddedSftpServer that can be used for testing.
     *
     * @return The embedded sftp server to use for testing
     */
    EmbeddedSftpServer build();

}
