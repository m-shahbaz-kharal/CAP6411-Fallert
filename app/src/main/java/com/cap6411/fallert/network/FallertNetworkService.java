package com.cap6411.fallert.network;

import android.content.Context;
import android.os.Handler;

import com.cap6411.fallert.devices.AlerteeDevices;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

public class FallertNetworkService {
    private Context mContext;
    public static final int SERVER_BROADCAST_PORT = 3255;
    public static final int SERVER_RECV_PORT = 3256;
    private Thread mServerAcceptClientThread;
    private Dictionary<String, Socket> mClientSockets = new Hashtable<>();
    private Thread mServerSendFallertEventToClientsThread;
    public static Queue<FallertEvent> mServerSendFallertEventQueue = new LinkedList<>();
    private Thread mServerRecvFallertEventFromClientsThread;
    public static Queue<FallertEvent> mServerRecvFallertEventQueue = new LinkedList<>();

    public FallertNetworkService(Context context){
        mContext = context;
    }
    public void startServerThread(String ipAddress, AlerteeDevices alerteeDevices) {
        mServerAcceptClientThread = new Thread(() -> {
            ServerSocket mSocket = StringNetwork.bindConnection(ipAddress, SERVER_BROADCAST_PORT);
            if (mSocket == null) return;
            try {
                while (true) {
                    Socket clientSocket = mSocket.accept();
                    mClientSockets.put("Unknown", clientSocket);
                    new Handler(mContext.getMainLooper()).post(() -> {
                        alerteeDevices.addDevice("Unknown", clientSocket.getInetAddress().getHostAddress());
                    });
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                try {StringNetwork.closeConnection(mSocket);}
                catch (Exception ignore) {}
            }
            finally {StringNetwork.closeConnection(mSocket);}
        });
        mServerAcceptClientThread.start();

        mServerSendFallertEventToClientsThread = new Thread(() -> {
            while (true) {
                FallertEvent event = mServerSendFallertEventQueue.poll();
                if (event != null) {
                    if (event.getEventType() == FallertEvent.FallertEventType.FALL) {
                        FallertEventFall fallEvent = (FallertEventFall) event;
                        for(Enumeration<String> ips = mClientSockets.keys(); ips.hasMoreElements();) {
                            String ip = ips.nextElement();
                            Socket mSocket = mClientSockets.get(ip);
                            String eventString = fallEvent.toString();
                            boolean success = StringNetwork.sendString(mSocket, eventString);
                            if(!success) {
                                mClientSockets.remove(ip);
                                new Handler(mContext.getMainLooper()).post(() -> {
                                    alerteeDevices.removeDevice(ip);
                                });
                            }
                        }
                    }
                    else if (event.getEventType() == FallertEvent.FallertEventType.INFORMATION){
                        FallertInformationEvent infoEvent = (FallertInformationEvent) event;
                        for(Enumeration<String> ips = mClientSockets.keys(); ips.hasMoreElements();) {
                            String ip = ips.nextElement();
                            Socket mSocket = mClientSockets.get(ip);
                            String eventString = infoEvent.toString();
                            boolean success = StringNetwork.sendString(mSocket, eventString);
                            if(!success) {
                                mClientSockets.remove(ip);
                                new Handler(mContext.getMainLooper()).post(() -> {
                                    alerteeDevices.removeDevice(ip);
                                });
                            }
                        }
                    }
                }
            }
        });
        mServerSendFallertEventToClientsThread.start();

        mServerRecvFallertEventFromClientsThread = new Thread(() -> {
            ServerSocket mSocket = StringNetwork.bindConnection(ipAddress, SERVER_RECV_PORT);
            if (mSocket == null) return;
            try {
                while (true) {
                    Socket clientSocket = mSocket.accept();
                    String msgString = StringNetwork.receiveString(clientSocket);
                    clientSocket.close();
                    if (msgString == null) throw new Exception("NULL msgString");
                    String eventType = msgString.split(":")[0];
                    switch (eventType) {
                        case "INFORMATION":
                            FallertInformationEvent infoEvent = FallertInformationEvent.parse(msgString);
                            if (infoEvent != null) mServerRecvFallertEventQueue.add(infoEvent);
                            break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                try {StringNetwork.closeConnection(mSocket);}
                catch (Exception ignore) {}
            }
            finally {StringNetwork.closeConnection(mSocket);}
        });
        mServerRecvFallertEventFromClientsThread.start();
    }

    public void removeClient(String clientIP) {
        mClientSockets.remove(clientIP);
    }

    public void stopServerThread() {
        if (mServerAcceptClientThread != null) mServerAcceptClientThread.interrupt();
        if (mServerSendFallertEventToClientsThread != null) mServerSendFallertEventToClientsThread.interrupt();
        for(Enumeration<String> ips = mClientSockets.keys(); ips.hasMoreElements();) {
            String ip = ips.nextElement();
            StringNetwork.closeConnection(mClientSockets.get(ip));
        }
        if (mServerRecvFallertEventFromClientsThread != null) mServerRecvFallertEventFromClientsThread.interrupt();

        mClientSockets = new Hashtable<>();
        mServerSendFallertEventQueue.clear();
        mServerRecvFallertEventQueue.clear();
    }
}