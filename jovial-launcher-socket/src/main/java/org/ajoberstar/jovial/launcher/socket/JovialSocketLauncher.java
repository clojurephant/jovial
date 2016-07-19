package org.ajoberstar.jovial.launcher.socket;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JovialSocketLauncher {
    public static void main(String[] args) throws Exception {
        try (SocketChannel channel = openSocket()) {
            TestExecutionListener listener = createListener(channel);
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(listener);

            ObjectInputStream inputStream = openInput();
            Map<String, List<String>> config = (Map<String, List<String>>) inputStream.readObject();
            Set<File> roots = config.get("classpathRoots").stream()
                .map(File::new)
                .collect(Collectors.toSet());

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClasspathRoots(roots))
                .build();

            launcher.execute(request);
        }
    }

    private static SocketChannel openSocket() {
        try {
            InetSocketAddress addr = new InetSocketAddress(0);
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(addr);
            // consumers should check the output stream for the port then connect
            int port = serverChannel.socket().getLocalPort();
            System.out.println(port);
            return serverChannel.accept();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open server socket.", e);
        }
    }

    private static TestExecutionListener createListener(SocketChannel channel) {
        try {
            OutputStream baseStream = Channels.newOutputStream(channel);
            ObjectOutputStream stream = new ObjectOutputStream(baseStream);
            return new SerializingListener(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open stream to socket client.", e);
        }
    }

    private static ObjectInputStream openInput() {
        try {
            return new ObjectInputStream(System.in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open input stream.", e);
        }
    }
}
