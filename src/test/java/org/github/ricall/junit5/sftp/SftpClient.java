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

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier.STRICT_CHECKING_OPTION;
import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_PASSWORD;
import static org.github.ricall.junit5.sftp.SftpServerConfiguration.DEFAULT_USERNAME;

public final class SftpClient {

    private static final JSch JSCH = new JSch();

    private final Session session;
    private final ChannelSftp channel;

    private SftpClient(final Configuration configuration) throws JSchException {
        session = JSCH.getSession(configuration.username, configuration.host, configuration.port);
        session.setConfig(STRICT_CHECKING_OPTION, "no");
        session.setPassword(configuration.password);
        session.connect(configuration.connectTimeout);

        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }

    public static Configuration builder() {
        return new Configuration();
    }

    public Session getSession() {
        return session;
    }

    public ChannelSftp getChannel() {
        return channel;
    }

    public void writeFile(final String path, final String content) throws SftpException {
        channel.put(new ByteArrayInputStream(content.getBytes(Charset.defaultCharset())), path);
    }

    public String readFile(final String path) throws SftpException, IOException {
        return IOUtils.toString(channel.get(path), Charset.defaultCharset());
    }

    public final static class Configuration {
        private String host = "localhost";
        private String username = DEFAULT_USERNAME;
        private String password = DEFAULT_PASSWORD;
        private int port = -1;
        private int connectTimeout = 15_000;

        public Configuration host(final String host) {
            this.host = host;
            return this;
        }

        public Configuration connectAs(final String username, final String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Configuration port(final int port) {
            this.port = port;
            return this;
        }

        public Configuration connectTimeout(final int timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public SftpClient build() throws JSchException {
            return new SftpClient(this);
        }
    }
}
