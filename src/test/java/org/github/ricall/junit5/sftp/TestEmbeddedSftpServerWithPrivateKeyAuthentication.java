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

import com.jcraft.jsch.JSchException;
import org.github.ricall.junit5.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.github.ricall.junit5.sftp.FileSystemResource.resourceAt;

public class TestEmbeddedSftpServerWithPrivateKeyAuthentication {

    @RegisterExtension
    public final EmbeddedSftpServer sftpServer = SftpServer.defaultSftpServer()
            .withPort(3022)
            .withAuthorizedKeys("/keys/publicKey.txt")
            .withResources(resourceAt("/customer/data").fromClasspathResource("/data"))
            .build();

    @Test
    public void verifyWeCanConnectToServer() throws Exception {
        try (SftpClient client = SftpClient.builder()
                .withIdentity("/keys/privateKey.txt")
                .port(3022)
                .build()) {

            assertThat(client.readFile("/customer/data/success/success1.xml")).isEqualTo("<xml>success</xml>");
        }
    }

    @Test
    public void verifyThatWeCannotConnectToTheServerWithoutThePrivateKey() {
        assertThatExceptionOfType(JSchException.class)
                .isThrownBy(() -> SftpClient.builder().port(3022).build())
                .withMessage("Auth fail");
    }

}
