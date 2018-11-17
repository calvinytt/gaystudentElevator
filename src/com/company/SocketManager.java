package com.company;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketManager {
    Socket socket;
    ServerSocket serverSocket;
    DataInputStream in;
    DataOutputStream out;
}
