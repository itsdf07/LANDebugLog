package com.sdf.aso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

/**
 * 封装局域网内部无线设备通讯连接：serviceSocket
 * Created by dengfu.su on 2016/12/13.
 */

public class LANLogDebugService {
    public static final int USER_ADD = 1;
    public static final int USER_REMOVE = 2;
    public static final int USER_REMOVE_All = 3;

    public interface ILogCallbackImpl {
        /**
         * 消息交互
         *
         * @param logs
         */
        void callbackLog(String logs);

        /**
         * 客户端交互
         *
         * @param user
         */
        void callbackUser(String user, int type);

        /**
         * 异常交互
         *
         * @param error
         * @param errorType
         */
        void errorCallback(String error, int errorType);
    }

    public LANLogDebugService(ILogCallbackImpl callback) {
        mILogCallbackImpl = callback;
    }

    private ILogCallbackImpl mILogCallbackImpl;
    private boolean isStart = false;//服务器是否已经启动
    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private ArrayList<ClientThread> clients;

    private String TAG = "dfsu";

    // 启动服务器
    public void serverStart(int max, int port) throws BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket, max);
            serverThread.start();
            isStart = true;
        } catch (BindException e) {
            isStart = false;
            throw new BindException("Port has been occupied, please change another!");//端口号已被占用，请换一个
        } catch (Exception e1) {
            e1.printStackTrace();
            isStart = false;
            throw new BindException("Start the server error！");//启动服务器异常
        }
    }

    // 服务器线程
    class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private int max;// 人数上限

        // 服务器线程的构造方法
        public ServerThread(ServerSocket serverSocket, int max) {
            this.serverSocket = serverSocket;
            this.max = max;
        }

        public void run() {
            while (true) {// 不停的等待客户端的链接
                try {
                    Socket socket = serverSocket.accept();
                    if (clients.size() >= max) {// 如果已达人数上限
                        BufferedReader r = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter w = new PrintWriter(socket
                                .getOutputStream());
                        // 接收客户端的基本用户信息
                        String inf = r.readLine();
                        StringTokenizer st = new StringTokenizer(inf, "@");
                        User user = new User(st.nextToken(), st.nextToken());
                        // 反馈连接成功信息
                        w.println("MAX@服务器：对不起，" + user.getName()
                                + user.getIp() + "，服务器在线人数已达上限，请稍后尝试连接！");
                        w.flush();
                        // 释放资源
                        r.close();
                        w.close();
                        socket.close();
                        continue;
                    }
                    ClientThread client = new ClientThread(socket);
                    client.start();// 开启对此客户端服务的线程
                    clients.add(client);
                    if (null != mILogCallbackImpl) {
                        mILogCallbackImpl.callbackUser(client.getUser().getName(), USER_ADD);// 更新在线列表
                    } else {
                        System.out.println("mILogCallbackImpl is null : USER_ADD");
                    }
                    callback2UI(client.getUser().getName() + client.getUser().getIp() + "Online!\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 为一个客户端服务的线程
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private User user;

        public BufferedReader getReader() {
            return reader;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public User getUser() {
            return user;
        }

        // 客户端线程的构造方法
        public ClientThread(Socket socket) {
            try {
                this.socket = socket;
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
                // 接收客户端的基本用户信息
                String inf = reader.readLine();
                StringTokenizer st = new StringTokenizer(inf, "@");
                user = new User(st.nextToken(), st.nextToken());
                // 反馈连接成功信息
                writer.println(user.getName() + user.getIp() + "With the server connection is successful");//与服务器连接成功!
                writer.flush();
                // 反馈当前在线用户信息
                if (clients.size() > 0) {
                    String temp = "";
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        temp += (clients.get(i).getUser().getName() + "/" + clients
                                .get(i).getUser().getIp())
                                + "@";
                    }
                    writer.println("USERLIST@" + clients.size() + "@" + temp);
                    writer.flush();
                }
                // 向所有在线用户发送该用户上线命令
                for (int i = clients.size() - 1; i >= 0; i--) {
                    clients.get(i).getWriter().println(
                            "ADD@" + user.getName() + user.getIp());
                    clients.get(i).getWriter().flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// 不断接收客户端的消息，进行处理。
            String message = null;
            while (true) {
                try {
                    message = reader.readLine();// 接收客户端消息
                    if (message.equals("CLOSE"))// 下线命令
                    {
                        callback2UI(this.getUser().getName() + this.getUser().getIp() + "Offline!\r\n");
                        // 断开连接释放资源
                        reader.close();
                        writer.close();
                        socket.close();

                        // 向所有在线用户发送该用户的下线命令
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            clients.get(i).getWriter().println(
                                    "DELETE@" + user.getName());
                            clients.get(i).getWriter().flush();
                        }
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackUser(user.getName(), USER_REMOVE);// 更新在线列表
                        } else {
                            System.out.println("mILogCallbackImpl is null :USER_REMOVE!");
                        }

                        // 删除此条客户端服务线程
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            if (clients.get(i).getUser() == user) {
                                ClientThread temp = clients.get(i);
                                clients.remove(i);// 删除此用户的服务线程
                                temp.stop();// 停止这条服务线程
                                return;
                            }
                        }
                    } else {
                        dispatcherMessage(message);// 转发消息
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 转发消息
        public void dispatcherMessage(String message) {
            System.out.println("dispatcherMessage : message = " + message);
//            StringTokenizer stringTokenizer = new StringTokenizer(message, "@");
//            String source = stringTokenizer.nextToken();
//            String owner = stringTokenizer.nextToken();
//            String content = stringTokenizer.nextToken();
//            message = source + "said:" + content;
            callback2UI(message + "\r\n");
//            if (owner.equals("ALL")) {// 群发
//                for (int i = clients.size() - 1; i >= 0; i--) {
//                    clients.get(i).getWriter().println(message);//多人发送
//                    clients.get(i).getWriter().flush();
//                }
//           }
        }
    }


    // 关闭服务器
    @SuppressWarnings("deprecation")
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// 停止服务器线程

            for (int i = clients.size() - 1; i >= 0; i--) {
                // 给所有在线用户发送关闭命令
                clients.get(i).getWriter().println("CLOSE");
                clients.get(i).getWriter().flush();
                // 释放资源
                clients.get(i).stop();// 停止此条为客户端服务的线程
                clients.get(i).reader.close();
                clients.get(i).writer.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// 关闭服务器端连接
            }
            if (null != mILogCallbackImpl) {
                mILogCallbackImpl.callbackUser("", USER_REMOVE_All);// 清空用户列表
            } else {
                System.out.println("mILogCallbackImpl is null : USER_REMOVE_All");
            }
            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
            isStart = true;
        }
    }

    // 群发服务器消息
    public void sendServerMessage(String message) {
        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).getWriter().println(message.trim());//多人发送
            clients.get(i).getWriter().flush();
        }
    }

    // 执行消息发送
    public void send(String msg) {
        if (null == mILogCallbackImpl) {
            System.out.println("mILogCallbackImpl is null : send");
        }
        if (!isStart) {
            mILogCallbackImpl.errorCallback("Server has not been started, cannot send messages！", JOptionPane.ERROR_MESSAGE);//服务器还未启动,不能发送消息
            return;
        }
        if (clients.size() == 0) {
            mILogCallbackImpl.errorCallback("No user online, can't send messages！", JOptionPane.ERROR_MESSAGE);//没有用户在线,不能发送消息
            return;
        }
        if (msg == null || msg.equals("")) {
            mILogCallbackImpl.errorCallback("The message cannot be empty！", JOptionPane.ERROR_MESSAGE);//消息不能为空
            return;
        }
        sendServerMessage(msg);// 群发服务器消息
        mILogCallbackImpl.callbackLog("The server said:" + msg + "\r\n");//服务器说
    }

    /**
     * 交互回调接口
     *
     * @param logs
     */
    private void callback2UI(String logs) {
        if (null != mILogCallbackImpl) {
            mILogCallbackImpl.callbackLog(logs);
        } else {
            System.out.println("mILogCallbackImpl is null : callbackLog");
        }
    }

    /**
     * 获取当前服务器状态：
     * @return
     */
    public boolean getServiceStatus(){
        return isStart;
    }

    //用户信息类
    public class User {
        private String name;
        private String ip;

        public User(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }
}
