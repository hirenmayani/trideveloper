package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.List;

import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class PassiveDataChannel implements Runnable, DataChannel {

    private static final int[] PORT_RANGE = getPortRange();

    private final List<Transfer> transferList = new ArrayList<Transfer>();

    private final String connectionInfo;

    private ServerSocket serverSocket;

    private Thread listenerThread;

    public PassiveDataChannel(InetAddress endpoint) throws IOException {
        serverSocket = createServerSocket(endpoint);
        StringBuilder info = new StringBuilder();
        InetAddress address = (endpoint == null) ?
                serverSocket.getInetAddress() : endpoint;
        for (byte addressByte : address.getAddress()) {
            info.append(addressByte & 0xff).append(',');
        }
        int port = serverSocket.getLocalPort();
        info.append(port / 256).append(',');
        info.append(port % 256);
        connectionInfo = info.toString();
        listenerThread = new Thread(this);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void run() {
        try {
            synchronized (transferList) {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketException ignore) {
                        break;
                    } catch (NullPointerException ignore) {
                        break;
                    }
                    while (transferList.isEmpty()) {
                        transferList.wait();
                    }
                    Transfer transfer = transferList.remove(0);
                    synchronized (transfer) {
                        try {
                            OutputStream output;
                            InputStream input;
                            if (transfer.isUpload()) {
                                output = transfer.getOutputStream();
                                input = socket.getInputStream();
                            } else {
                                output = socket.getOutputStream();
                                input = transfer.getInputStream();
                            }
                            byte[] buf = new byte[65536];
                            int count;
                            while ((count = input.read(buf)) != -1) {
                                output.write(buf, 0, count);
                            }
                            output.flush();
                            input.close();
                            output.close();
                            transfer.setStatus(SC_CLOSING_DATA);
                            transfer.setMessage("Transfer complete");
                        } catch (IOException err) {
                            transfer.setStatus(SC_CLOSED_TRANSFER_ABORTED);
                            transfer.setMessage("Transfer aborted");
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException ignore) { }
                        }
                        transfer.notifyAll();
                    }
                }
            }
        } catch (InterruptedException ex) {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void send(Transfer transfer) throws IOException {
        if (isClosed()) {
            throw new IOException("Passive Data Connection closed.");
        }
        synchronized (transfer) {
            synchronized (transferList) {
                transferList.add(transfer);
                transferList.notifyAll();
            }
            try {
                transfer.wait();
            } catch (InterruptedException ex) {
                throw new IOException("Transfer interrupted.");
            }
        }
    }

    public boolean isClosed() {
        return (serverSocket == null);
    }

    public void close() {
        if (isClosed()) return;
        try {
            listenerThread.interrupt();
        } catch (Exception ignore) { }
        try {
            if (!serverSocket.isClosed()) serverSocket.close();
        } catch (Exception ignore) { }
        serverSocket = null;
        listenerThread = null;
    }

    public String getConnectionInfo() {
        return connectionInfo;
    }

    private static ServerSocket createServerSocket(InetAddress endpoint)
            throws IOException {
        if (!Configuration.getBoolean(ISOLATE_DATA_CHANNEL_PROPERTY)) {
            endpoint = null;
        }
        if (PORT_RANGE == null) return new ServerSocket(0, 0, endpoint);
        for (int port = PORT_RANGE[0], maxPort = PORT_RANGE[1];
                port <= maxPort; port++) {
            try {
                return new ServerSocket(port, 0, endpoint);
            } catch (BindException ignore) { }
        }
        throw new IOException("No ports available in configured range.");
    }

    private static int[] getPortRange() {
        String passivePortRange =
                Configuration.getString(PASSIVE_PORT_RANGE_PROPERTY);
        if (passivePortRange == null ||
                "".equals(passivePortRange = passivePortRange.trim())) {
            return null;
        }
        try {
            String[] bounds = passivePortRange.split("[ -]+");
            if (bounds.length != 2) throw new Exception();
            int first = Integer.parseInt(bounds[0]);
            int second = Integer.parseInt(bounds[1]);
            if (first < 1 || first > 65535) throw new Exception();
            if (second < 1 || second > 65535) throw new Exception();
            if (second < first) {
                int tmp = first;
                first = second;
                second = tmp;
            }
            return new int[] { first, second };
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid Passive Port range: " +
                    passivePortRange);
        }
    }

}
