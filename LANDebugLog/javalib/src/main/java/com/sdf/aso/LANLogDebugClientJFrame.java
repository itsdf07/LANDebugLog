package com.sdf.aso;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * Created by dengfu.su on 2016/12/5.
 */
public class LANLogDebugClientJFrame implements LANLogDebugClient.ILogCallbackImpl {
    private JFrame mJFrame;
    private JTextArea mTextArea;
    private JTextField mTextField;
    private JTextField mPort;
    private JTextField mHost;
    private JTextField mClientName;
    private JButton mConnection;
    private JButton mDisConnection;
    private JButton mMessage2Send;
    private JPanel mNorthPanel;
    private JPanel mSouthPanel;
    private JScrollPane mRightScroll;

    private DefaultListModel mListModel;

    private LANLogDebugClient mLANLogDebugClient;

    // 主方法,程序入口
    public static void main(String[] args) {
        new LANLogDebugClientJFrame();
    }

    // 执行发送
    public void send() {
        if (!mLANLogDebugClient.getConnectionStatus()) {
            errorCallback("Haven't connect to the server to send messages!", JOptionPane.ERROR_MESSAGE);//还没有连接服务器，无法发送消息！
            return;
        }
        String message = mTextField.getText().trim();
        if (message == null || message.equals("")) {
            errorCallback("The message cannot be empty!", JOptionPane.ERROR_MESSAGE);//消息不能为空
            return;
        }
        mLANLogDebugClient.sendMessage(mJFrame.getTitle() + "@" + "ALL" + "@" + message);
        mTextField.setText(null);
    }

    // 构造方法
    public LANLogDebugClientJFrame() {
        mJFrame = new JFrame("LANLogDebugClientJFrame");
        mTextArea = new JTextArea();
        mTextArea.setEditable(false);
        mTextArea.setForeground(Color.blue);
        mTextField = new JTextField();
        mPort = new JTextField("6666");
        mHost = new JTextField("127.0.0.1");
        mClientName = new JTextField("aso");
        mConnection = new JButton("Conn");
        mDisConnection = new JButton("Disconn ");
        mMessage2Send = new JButton("Send");
        mListModel = new DefaultListModel();

        mNorthPanel = new JPanel();
        mNorthPanel.setLayout(new GridLayout(1, 7));
        mNorthPanel.add(new JLabel("Port"));
        mNorthPanel.add(mPort);
        mNorthPanel.add(new JLabel("Host"));
        mNorthPanel.add(mHost);
        mNorthPanel.add(new JLabel("Name"));
        mNorthPanel.add(mClientName);
        mNorthPanel.add(mConnection);
        mNorthPanel.add(mDisConnection);
        mNorthPanel.setBorder(new TitledBorder("Connection info"));

        mRightScroll = new JScrollPane(mTextArea);
        mRightScroll.setBorder(new TitledBorder("Message display area"));
        mSouthPanel = new JPanel(new BorderLayout());
        mSouthPanel.add(mTextField, "Center");
        mSouthPanel.add(mMessage2Send, "East");
        mSouthPanel.setBorder(new TitledBorder("Message to Send"));

        // 更改JFrame的图标：
        // mJFrame.setIconImage(Toolkit.getDefaultToolkit().createImage(LANLogDebugClientJFrame.class.getResource("qq.png")));
        mJFrame.setLayout(new BorderLayout());
        mJFrame.add(mNorthPanel, "North");
        mJFrame.add(mRightScroll, "Center");
        mJFrame.add(mSouthPanel, "South");
        mJFrame.setSize(600, 400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        mJFrame.setLocation((screen_width - mJFrame.getWidth()) / 2, (screen_height - mJFrame.getHeight()) / 2);
        mJFrame.setVisible(true);

        // 写消息的文本框中按回车键时事件
        mTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        // 单击发送按钮时事件
        mMessage2Send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // 单击连接按钮时事件
        mConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port;
                if (mLANLogDebugClient.getConnectionStatus()) {
                    errorCallback("Has been in the connected state, don't repeat connection!", JOptionPane.ERROR_MESSAGE);//已处于连接上状态，不要重复连接!
                    return;
                }
                try {
                    try {
                        port = Integer.parseInt(mPort.getText().trim());
                    } catch (NumberFormatException e2) {
                        throw new Exception("端口号不符合要求!端口为整数!");
                    }
                    String hostIp = mHost.getText().trim();
                    String name = mClientName.getText().trim();
                    if (name.equals("") || hostIp.equals("")) {
                        throw new Exception("姓名、服务器IP不能为空!");
                    }
                    boolean flag = mLANLogDebugClient.connectServer(port, hostIp, name);
                    if (flag == false) {
                        throw new Exception("与服务器连接失败!");
                    }
                    mJFrame.setTitle(name);
                    JOptionPane.showMessageDialog(mJFrame, "Successful connection");//成功连接!
                } catch (Exception exc) {
                    callbackLog(exc.getMessage().toString());
                    errorCallback("Error", JOptionPane.ERROR_MESSAGE);//已处于连接上状态，不要重复连接!
                }
            }
        });

        // 单击断开按钮时事件
        mDisConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!mLANLogDebugClient.getConnectionStatus()) {
                    errorCallback("Is in off state, don't repeat disconnect!", JOptionPane.ERROR_MESSAGE);//已处于断开状态，不要重复断开!
                    return;
                }
                try {
                    boolean flag = mLANLogDebugClient.closeConnection();// 断开连接
                    if (flag == false) {
                        throw new Exception("断开连接发生异常！");
                    }
                    JOptionPane.showMessageDialog(mJFrame, "Successful disconnect!");//成功断开
                } catch (Exception exc) {
                    callbackLog(exc.getMessage().toString());
                    errorCallback("Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 关闭窗口时事件
        mJFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (mLANLogDebugClient.getConnectionStatus()) {
                    mLANLogDebugClient.closeConnection();// 关闭连接
                }
                System.exit(0);// 退出程序
            }
        });
        mLANLogDebugClient = new LANLogDebugClient(this);
    }


    @Override
    public void callbackLog(String logs) {
        mTextArea.append(logs + "\r\n");
    }

    @Override
    public void callbackUser(String user, int type) {
        if (type == LANLogDebugService.USER_ADD) {
            mListModel.addElement(user);
        } else if (type == LANLogDebugService.USER_REMOVE) {
            mListModel.removeElement(user);
        } else if (type == LANLogDebugService.USER_REMOVE_All) {
            mListModel.removeAllElements();
        }
    }

    @Override
    public void errorCallback(String error, int errorType) {
        JOptionPane.showMessageDialog(mJFrame, error, "error", JOptionPane.ERROR_MESSAGE);
    }
}
