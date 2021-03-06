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

import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.github.ricall.junit5.sftp.FileSystemResource;
import org.github.ricall.junit5.sftp.SftpServerConfiguration;

import java.util.*;

public final class DefaultSftpServerConfiguration implements SftpServerConfiguration, PasswordAuthenticator {

    private static final int MAX_PORT = 65_535;
    private transient int port;
    private final Map<String, String> users = new LinkedHashMap<>();
    private final List<FileSystemResource> resources = new ArrayList<>();
    private transient KeyPairProvider keyPairProvider = new SimpleGeneratorHostKeyProvider();

    private DefaultSftpServerConfiguration() {
    }

    public static DefaultSftpServerConfiguration configuration() {
        return new DefaultSftpServerConfiguration();
    }

    @Override
    public DefaultSftpServerConfiguration withPort(final int port) {
        if (port < 0 || port > MAX_PORT) {
            throw new IllegalArgumentException("Port needs to be between 1-65535");
        }
        this.port = port;
        return this;
    }

    @Override
    public DefaultSftpServerConfiguration withUser(final String username, final String password) {
        this.users.put(username, password);
        return this;
    }

    @Override
    public DefaultSftpServerConfiguration withResources(final List<FileSystemResource> resources) {
        this.resources.addAll(resources);
        return this;
    }

    @Override
    public DefaultSftpServerConfiguration withKeyPairProvider(final KeyPairProvider keyPairProvider) {
        this.keyPairProvider = keyPairProvider;
        return this;
    }

    public int getPort() {
        return port;
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
        throw new UnsupportedOperationException("Password change not supported");
    }

    public Map<String, String> getUsers() {
        return users;
    }

    public List<FileSystemResource> getResources() {
        return resources;
    }

    public KeyPairProvider getKeyPairProvider() {
        return keyPairProvider;
    }

}