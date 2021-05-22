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

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.github.ricall.junit5.sftp.EmbeddedSftpServer;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Provides an EmbeddedSftpServer that can be used with JUnit 5.
 */
@RequiredArgsConstructor
public class LifecycleAwareEmbeddedSftpServer
        implements EmbeddedSftpServer, BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    public static final String NAMESPACE = EmbeddedSftpServer.class.getName();
    public static final String SERVER_KEY = DefaultEmbeddedSftpServer.class.getName();

    private final SftpConfiguration configuration;

    @Delegate
    private transient EmbeddedSftpServer server;
    private transient boolean serverPerMethod;

    @Override
    public void beforeAll(final ExtensionContext context) {
        initialiseServer(context);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        if (this.server == null) {
            serverPerMethod = true;
            initialiseServer(context);
        } else {
            server.resetFileSystem();
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (serverPerMethod) {
            cleanupServer(context);
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (server != null) {
            cleanupServer(context);
        }
    }

    private void initialiseServer(final ExtensionContext context) {
        final Store store = context.getRoot().getStore(Namespace.create(NAMESPACE));
        DefaultEmbeddedSftpServer server = store.get(SERVER_KEY, DefaultEmbeddedSftpServer.class);

        if (server == null) {
            server = new DefaultEmbeddedSftpServer(configuration);
            store.put(SERVER_KEY, server);
            server.startServer();
        }
        this.server = server;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private void cleanupServer(final ExtensionContext context) {
        final Store store = context.getRoot().getStore(Namespace.create(NAMESPACE));

        store.get(SERVER_KEY, DefaultEmbeddedSftpServer.class).stopServer();
        store.remove(SERVER_KEY);
        this.server = null;
    }

}
