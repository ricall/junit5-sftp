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

import org.github.ricall.junit5.sftp.implementation.DefaultSftpEmbeddedServer;
import org.github.ricall.junit5.sftp.implementation.DefaultSftpServerConfiguration;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;

public class SftpEmbeddedServerExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {
    public static final String NAMESPACE = SftpEmbeddedServer.class.getName();
    public static final String SERVER_KEY = DefaultSftpEmbeddedServer.class.getName();

    @Override
    public void beforeEach(final ExtensionContext context) {
        if (getServer(context) == null) {
            final DefaultSftpEmbeddedServer server = new DefaultSftpEmbeddedServer(getConfigurationWithDefault(context));
            context.getRoot().getStore(Namespace.create(NAMESPACE))
                    .put(SERVER_KEY, server);
            server.startServer();
        }
    }

    private DefaultSftpEmbeddedServer getServer(final ExtensionContext context) {
        return context.getRoot().getStore(Namespace.create(NAMESPACE))
                .get(SERVER_KEY, DefaultSftpEmbeddedServer.class);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        getServer(context).resetFileSystem();
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        getServer(context).stopServer();
        context.getRoot()
                .getStore(Namespace.create(NAMESPACE, context))
                .remove(SERVER_KEY);
    }

    private DefaultSftpServerConfiguration getConfigurationWithDefault(final ExtensionContext context) {
        return context.getTestInstance()
                .flatMap(this::getConfiguration)
                .orElse(DefaultSftpServerConfiguration.configuration());
    }

    private Optional<DefaultSftpServerConfiguration> getConfiguration(final Object instance) {
        return ReflectionUtils.findFields(instance.getClass(), this::isSftpConfiguration, TOP_DOWN).stream()
                .map(field -> getConfigurationFromField(field, instance))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean isSftpConfiguration(final Field field) {
        return field.getType() == SftpServerConfiguration.class;
    }

    private DefaultSftpServerConfiguration getConfigurationFromField(final Field field, final Object testInstance) {
        try {
            return (DefaultSftpServerConfiguration) field.get(testInstance);
        } catch (IllegalAccessException iae) {
            throw new IllegalArgumentException("Unable to access field " + field.getName(), iae);
        }
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        final Parameter parameter = parameterContext.getParameter();

        return isSftpServerParameter(parameter);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext context) {
        if (isSftpServerParameter(parameterContext.getParameter())) {
            return getServer(context);
        }
        return null;
    }

    private boolean isSftpServerParameter(final Parameter parameter) {
        return parameter.getType() == SftpEmbeddedServer.class;
    }

}
