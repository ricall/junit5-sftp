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
import org.github.ricall.junit5.sftp.api.ServerConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DefaultServerConfigurationTest {

    private static final String USER = "user";
    private static final String PASS = "pass";
    private static final String INVALID_PASSWORD = "invalid";
    private static final String NEW_PASSWORD = "new password";

    private final DefaultSftpServerConfiguration configuration =
            (DefaultSftpServerConfiguration) ServerConfiguration.configuration();

    @Test
    public void verifyWeCanSetThePort() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> configuration.withPort(0))
                .withMessage("Port needs to be between 1-65535");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> configuration.withPort(65_536))
                .withMessage("Port needs to be between 1-65535");

        assertThat(configuration.withPort(1)).isSameAs(configuration);
        assertThat(configuration.getPort()).isEqualTo(1);

        assertThat(configuration.withPort(65_535)).isSameAs(configuration);
        assertThat(configuration.getPort()).isEqualTo(65_535);
    }

    @Test
    public void verifyWeCanAddUsers() {
        assertThat(configuration.withUser(USER, PASS)).isSameAs(configuration);
        assertThat(configuration.getUsers()).containsEntry(USER, PASS);
    }

    @Test
    public void verifyWeCanAddResources() {
        final FileSystemResource resource = Mockito.mock(FileSystemResource.class);
        assertThat(configuration.withResources(singletonList(resource))).isSameAs(configuration);
        assertThat(configuration.getResources()).contains(resource);
    }

    @Test
    public void verifyUserAuthentication() {
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(false);
        configuration.withUser(USER, PASS);
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(true);
    }

    @Test
    public void verifyFailedChangePasswordKeepsOldPassword() {
        configuration.withUser(USER, PASS);
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(true);

        assertThat(configuration.handleClientPasswordChangeRequest(
                null,
                USER,
                INVALID_PASSWORD,
                NEW_PASSWORD)).isEqualTo(false);
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(true);
    }

    @Test
    public void verifyChangePasswordUpdatesPassword() {
        configuration.withUser(USER, PASS);
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(true);

        assertThat(configuration.handleClientPasswordChangeRequest(
                null,
                USER,
                PASS,
                NEW_PASSWORD)).isEqualTo(true);
        assertThat(configuration.authenticate(USER, PASS, null)).isEqualTo(false);
        assertThat(configuration.authenticate(USER, NEW_PASSWORD, null)).isEqualTo(true);
    }

}
