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

import lombok.Getter;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.github.ricall.junit5.sftp.EmbeddedSftpServer;
import org.github.ricall.junit5.sftp.FileSystemResource;
import org.github.ricall.junit5.sftp.SftpServer;

import java.nio.file.Path;
import java.util.*;

import static org.github.ricall.junit5.sftp.implementation.ServerUtils.classpathResourceToPath;

@Getter
@SuppressWarnings("PMD.TooManyMethods")
public final class SftpConfiguration implements SftpServer, PasswordAuthenticator {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65_535;

    private int port;
    private final Map<String, String> users = new LinkedHashMap<>();
    private final List<FileSystemResource> resources = new ArrayList<>();
    private KeyPairProvider keyPairProvider = new SimpleGeneratorHostKeyProvider();
    private Path authorizedKeys;

    public static SftpConfiguration configuration() {
        return new SftpConfiguration();
    }

    @Override
    public SftpConfiguration withPort(final int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Port needs to be between 1-65535");
        }
        this.port = port;
        return this;
    }

    @Override
    public SftpConfiguration withUser(final String username, final String password) {
        this.users.put(username, password);
        return this;
    }

    @Override
    public SftpConfiguration withResources(final List<FileSystemResource> resources) {
        this.resources.addAll(resources);
        return this;
    }

    @Override
    public SftpConfiguration withKeyPairProvider(final KeyPairProvider keyPairProvider) {
        this.keyPairProvider = keyPairProvider;
        return this;
    }

    @Override
    public SftpConfiguration withAuthorizedKeys(final String classpathResource) {
        return withAuthorizedKeys(classpathResourceToPath(classpathResource));
    }

    @Override
    public SftpConfiguration withAuthorizedKeys(final Path authorizedKeys) {
        this.authorizedKeys = authorizedKeys;
        return this;
    }

    @Override
    public boolean authenticate(
            final String username,
            final String password,
            final ServerSession session) {
        return Objects.equals(password, users.get(username));
    }

    @Override
    public boolean handleClientPasswordChangeRequest(
            final ServerSession session,
            final String username,
            final String oldPassword,
            final String newPassword) {
        if (authenticate(username, oldPassword, session)) {
            users.put(username, newPassword);
            return true;
        }
        return false;
    }

    @Override
    public EmbeddedSftpServer build() {
        return new LifecycleAwareEmbeddedSftpServer(this);
    }

    public boolean noAuthenticationDefined() {
        return authorizedKeys == null && users.isEmpty();
    }

}
