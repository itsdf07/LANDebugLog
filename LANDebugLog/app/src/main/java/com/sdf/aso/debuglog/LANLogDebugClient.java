package com.sdf.aso.debuglog;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 封装局域网内部无线设备通讯连接：Socket
 * Created by dengfu.su on 2016/12/13.
 */

public class LANLogDebugClient {
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

    public LANLogDebugClient(ILogCallbackImpl callback) {
        mILogCallbackImpl = callback;
    }

    private ILogCallbackImpl mILogCallbackImpl;
    private Socket mSocket;
    private PrintWriter mPrintWriter;
    private BufferedReader mBfreader;
    private MessageThread mMessageThread;// 负责接收消息的线程
    private Map<String, User> mOnLineUsers = new HashMap<String, User>();// 所有在线用户

    private boolean isConnected = false;//是否已经连上服务器了

    private String TAG = "dfsu";

    /**
     * 连接服务器
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer(int port, String hostIp, String name) {
        // 连接服务器
        try {
            mSocket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
            mPrintWriter = new PrintWriter(mSocket.getOutputStream());
            mBfreader = new BufferedReader(new InputStreamReader(mSocket.getInputStream(),"UTF-8"));
            // 发送客户端用户基本信息(用户名和ip地址)
            sendMessage(name + "@" + mSocket.getLocalAddress().toString());
            // 开启接收消息的线程
            mMessageThread = new MessageThread(mBfreader);
            mMessageThread.start();
            isConnected = true;// 已经连接上了
            return true;
        } catch (Exception e) {
            if (null != mILogCallbackImpl) {
                mILogCallbackImpl.callbackLog("And the port number is:" + port + ",Host is" + hostIp + " the server connection failed!");
                mILogCallbackImpl.callbackLog("e:" + e.toString());
            } else {
                System.out.println("mILogCallbackImpl is null : connectServer \n" + e.toString());
            }
            isConnected = false;// 未连接上
            return false;
        }
    }

    /**
     * 发送消息
     *
     * @param message
     */
    public void sendMessage(String message) {
        System.out.println(TAG + ":message = " + message + ",mPrintWriter = " + mPrintWriter);
        if (null == mPrintWriter){
            return;
        }
        mPrintWriter.println(message);
        mPrintWriter.flush();
    }

    /**
     * 客户端主动关闭连接
     */
    @SuppressWarnings("deprecation")
    public synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE");// 发送断开连接命令给服务器
            mMessageThread.stop();// 停止接受消息线程
            // 释放资源
            if (mBfreader != null) {
                mBfreader.close();
            }
            if (mPrintWriter != null) {
                mPrintWriter.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
            isConnected = false;
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            isConnected = true;
            return false;
        }
    }

    // 不断接收消息的线程
    class MessageThread extends Thread {
        private BufferedReader reader;

        // 接收消息线程的构造方法
        public MessageThread(BufferedReader reader) {
            this.reader = reader;
        }

        // 被动的关闭连接
        public synchronized void closeCon() throws Exception {
            if (null == mILogCallbackImpl) {
                System.out.println("mILogCallbackImpl is null : closeCon");
                return;
            }
            mILogCallbackImpl.callbackUser("", USER_REMOVE_All);// 清空用户列表

            // 被动的关闭连接释放资源
            if (reader != null) {
                reader.close();
            }
            if (mPrintWriter != null) {
                mPrintWriter.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
            isConnected = false;// 修改状态为断开
        }

        @Override
        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(message, "/@");
                    String command = stringTokenizer.nextToken();// 命令
                    System.out.println("MessageThread run :command ================== " + command + ", message = " + message);
                    if (command.equals("CLOSE"))// 服务器已关闭命令
                    {
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackLog("The server has been closed!");//服务器已关闭
                        } else {
                            System.out.println("mILogCallbackImpl is null : MessageThread run CLOSE ");
                        }
                        closeCon();// 被动的关闭连接
                        return;// 结束线程
                    } else if (command.equals("ADD")) {// 有用户上线更新在线列表
                        String username = "";
                        String userIp = "";
                        if ((username = stringTokenizer.nextToken()) != null
                                && (userIp = stringTokenizer.nextToken()) != null) {
                            User user = new User(username, userIp);
                            mOnLineUsers.put(username, user);
                            if (null != mILogCallbackImpl) {
                                mILogCallbackImpl.callbackUser(username, USER_ADD);
                            } else {
                                System.out.println("mILogCallbackImpl is null : MessageThread run ADD");
                            }
                        }
                    } else if (command.equals("DELETE")) {// 有用户下线更新在线列表
                        String username = stringTokenizer.nextToken();
                        User user = mOnLineUsers.get(username);
                        mOnLineUsers.remove(user);
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackUser(username, USER_ADD);
                        } else {
                            System.out.println("mILogCallbackImpl is null : MessageThread run DELETE");
                        }
                    } else if (command.equals("MAX")) {// 人数已达上限
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackLog(stringTokenizer.nextToken() + stringTokenizer.nextToken());
                        } else {
                            System.out.println("mILogCallbackImpl is null : MessageThread run MAX ");
                        }
                        closeCon();// 被动的关闭连接
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackLog("The server buffer is full!");//服务器缓冲区已满!
                        } else {
                            System.out.println("mILogCallbackImpl is null : MessageThread run MAX");
                        }
                        return;// 结束线程
                    } else {// 普通消息
                        TAG = message;
                        if (null != mILogCallbackImpl) {
                            mILogCallbackImpl.callbackLog(message);
                        } else {
                            System.out.println("mILogCallbackImpl is null : MessageThread run Normal ");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取连接服务器状态
     *
     * @return
     */
    public boolean getConnectionStatus() {
        return isConnected;
    }

    /**
     * @return 过滤的TAG
     */
    public String getTAG() {
        return TAG;
    }

    /**
     * Created by dengfu.su on 2016/12/3.
     */
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
