package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.List;

import static com.trideveloper.ftp.FtpStatus.*;

public class PassiveDataChannel implements Runnable, DataChannel {

    private final List<Transfer> transferList = new ArrayList<Transfer>();

    private final String connectionInfo;

    private ServerSocket serverSocket;

    private Thread listenerThread;

    public PassiveDataChannel(InetAddress endpoint) throws IOException {
        serverSocket = new ServerSocket(0, 0, endpoint);
        StringBuilder info = new StringBuilder();
        InetAddress address = serverSocket.getInetAddress();
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

}
