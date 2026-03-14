package lineage;

import java.net.BindException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class AuthThread extends Thread {
	public static final int AUTH_SERVER_PORT = 13000;

	public static boolean isPortAvailable(int port) {
		ServerSocket probe = null;
		try {
			probe = new ServerSocket();
			probe.bind(new InetSocketAddress(port));
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (probe != null) {
				try {
					probe.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	public void run() {
		java.net.Socket socket = null;

		setName("AuthThread");

		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.bind(new InetSocketAddress(AUTH_SERVER_PORT));

			while (true) {
				socket = serverSocket.accept();
				AuthClient authclient = new AuthClient(socket);
				authclient.start();
			}
		} catch (BindException e) {
			System.err.println(String.format("[ERROR] Auth port %d is already in use. Stop existing instance and retry.", AUTH_SERVER_PORT));
		} catch (IOException e) {
			System.err.println(String.format("[ERROR] AuthThread failed on port %d: %s", AUTH_SERVER_PORT, e.getMessage()));
		}
	}
}
