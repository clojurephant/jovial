package org.ajoberstar.jovial.launcher.socket;

import org.junit.platform.launcher.TestExecutionListener;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class JovialSocketLauncher {
    public static void main(String[] args) {
        ServerSocketChannel serverChannel = openServerSocket();
        TestExecutionListener listener = createListener(serverChannel);


    }

    private static ServerSocketChannel openServerSocket() {
        try {
            InetSocketAddress addr = new InetSocketAddress(0);
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(addr);
            // consumers should check the output stream for the port then connect
            System.out.println(addr.getPort());
            return serverChannel;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open server socket.", e);
        }

    }

    private static TestExecutionListener createListener(ServerSocketChannel serverChannel) {
        try {
            SocketChannel channel = serverChannel.accept();
            OutputStream baseStream = Channels.newOutputStream(channel);
            ObjectOutputStream stream = new ObjectOutputStream(baseStream);
            return new SerializingListener(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open stream to socket client.", e);
        }
    }
}
