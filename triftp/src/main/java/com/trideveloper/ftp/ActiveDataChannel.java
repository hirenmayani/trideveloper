package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.List;

import static com.trideveloper.ftp.FtpStatus.*;

public class ActiveDataChannel implements Runnable, DataChannel {

    private final List<Transfer> transferList = new ArrayList<Transfer>();

    private final String connectionInfo;

    private Socket socket;

    private InetAddress localAddress;

    private InetAddress address;

    private int port;

    private Thread listenerThread;

    public ActiveDataChannel(InetAddress localAddress,
            InetAddress address, int port) throws IOException {
        this.localAddress = localAddress;
        this.address = address;
        this.port = port;
        StringBuilder info = new StringBuilder();
        for (byte addressByte : address.getAddress()) {
            info.append(addressByte & 0xff).append(',');
        }
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
                    while (transferList.isEmpty()) {
                        transferList.wait();
                    }
                    try {
                        socket = new Socket(address, port, localAddress, 0);
                    } catch (SocketException ignore) {
                        break;
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
            throw new IOException("Active Data Connection closed.");
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
        return (address == null);
    }

    public void close() {
        if (isClosed()) return;
        try {
            listenerThread.interrupt();
        } catch (Exception ignore) { }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignore) { }
        socket = null;
        address = null;
        localAddress = null;
        port = 0;
        listenerThread = null;
    }

    public String getConnectionInfo() {
        return connectionInfo;
    }

}
