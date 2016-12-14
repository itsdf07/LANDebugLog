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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class LANLogServiceSocketJFrame implements LANLogDebugService.ILogCallbackImpl {

    private JFrame mJFrame;
    private JTextArea mContentArea;
    private JTextField mMessageToSend;
    private JTextField mMaxClient;
    private JTextField mPort;
    private JButton mStartService;
    private JButton mStopService;
    private JButton mSend;
    private JPanel mNorthPanel;
    private JPanel mSouthPanel;
    private JScrollPane mRightPanel;
    private JScrollPane mLeftPanel;
    private JSplitPane mCenterSplit;
    private JList mUserList;
    private DefaultListModel mListModel;

    private LANLogDebugService mLANLogDebug;


    // 主方法,程序执行入口
    public static void main(String[] args) {
        new LANLogServiceSocketJFrame();
    }

    // 构造放法
    public LANLogServiceSocketJFrame() {
        mJFrame = new JFrame("LANLogServiceSocketJFrame");
        // 更改JFrame的图标：
        //mJFrame.setIconImage(Toolkit.getDefaultToolkit().createImage(LANLogDebugClientJFrame.class.getResource("qq.png")));
//        mJFrame.setIconImage(Toolkit.getDefaultToolkit().createImage(LANLogServiceSocketJFrame.class.getResource("qq.png")));
        mContentArea = new JTextArea();
        mContentArea.setEditable(false);
        mContentArea.setForeground(Color.blue);
        mMessageToSend = new JTextField();
        mMaxClient = new JTextField("30");
        mPort = new JTextField("6666");
        mStartService = new JButton("Start");
        mStopService = new JButton("Stop");
        mSend = new JButton("Send");
        mStopService.setEnabled(false);
        mListModel = new DefaultListModel();
        mUserList = new JList(mListModel);

        mSouthPanel = new JPanel(new BorderLayout());
        mSouthPanel.setBorder(new TitledBorder("Message to send"));//写消息
        mSouthPanel.add(mMessageToSend, "Center");
        mSouthPanel.add(mSend, "East");
        mLeftPanel = new JScrollPane(mUserList);
        mLeftPanel.setBorder(new TitledBorder("Online"));//在线用户

        mRightPanel = new JScrollPane(mContentArea);
        mRightPanel.setBorder(new TitledBorder("Message display area"));//消息显示区

        mCenterSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mLeftPanel,
                mRightPanel);
        mCenterSplit.setDividerLocation(100);
        mNorthPanel = new JPanel();
        mNorthPanel.setLayout(new GridLayout(1, 6));
        mNorthPanel.add(new JLabel("Max LANLogDebugClientJFrame"));//人数上限
        mNorthPanel.add(mMaxClient);
        mNorthPanel.add(new JLabel("Port"));//端口
        mNorthPanel.add(mPort);
        mNorthPanel.add(mStartService);
        mNorthPanel.add(mStopService);
        mNorthPanel.setBorder(new TitledBorder("Configuration"));//配置信息

        mJFrame.setLayout(new BorderLayout());
        mJFrame.add(mNorthPanel, "North");
        mJFrame.add(mCenterSplit, "Center");
        mJFrame.add(mSouthPanel, "South");
        mJFrame.setSize(600, 400);
//        mJFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());//设置全屏
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        mJFrame.setLocation((screen_width - mJFrame.getWidth()) / 2,
                (screen_height - mJFrame.getHeight()) / 2);
        mJFrame.setVisible(true);

        // 关闭窗口时事件
        mJFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (mLANLogDebug.getServiceStatus()) {
                    mLANLogDebug.closeServer();// 关闭服务器
                }
                System.exit(0);// 退出程序
            }
        });

        // 单击发送按钮时事件
        mSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String message = mMessageToSend.getText().trim();
                mLANLogDebug.send(message);
            }
        });

        // 单击启动服务器按钮时事件
        mStartService.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (mLANLogDebug.getServiceStatus()) {
                    errorCallback("The server has been started in state, don't repeat",JOptionPane.ERROR_MESSAGE);//服务器已处于启动状态，不要重复启动
                    return;
                }
                int max;
                int port;
                try {
                    try {
                        max = Integer.parseInt(mMaxClient.getText());
                    } catch (Exception e1) {
                        throw new Exception("Max limit");
                    }
                    if (max <= 0) {
                        throw new Exception("Num Need Integer");
                    }
                    try {
                        port = Integer.parseInt(mPort.getText());
                    } catch (Exception e1) {
                        throw new Exception("Port need Integer");
                    }
                    if (port <= 0) {
                        throw new Exception("Port need > 0");
                    }
                    mLANLogDebug.serverStart(max, port);
                    mContentArea.append("The server has successfully started port:" + port
                            + "\r\n");//服务器已成功启动!人数上限
                    JOptionPane.showMessageDialog(mJFrame, "The server starts successfully!");//服务器成功启动
                    mStartService.setEnabled(false);
                    mPort.setEnabled(false);
                    mStopService.setEnabled(true);
                } catch (Exception exc) {
                    errorCallback(exc.getMessage().toString(),JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 单击停止服务器按钮时事件
        mStopService.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!mLANLogDebug.getServiceStatus()) {
                    errorCallback("The server has not yet started, need not stop!",JOptionPane.ERROR_MESSAGE);//服务器还未启动，无需停止
                    return;
                }
                try {
                    mLANLogDebug.closeServer();
                    mStartService.setEnabled(true);
                    mPort.setEnabled(true);
                    mStopService.setEnabled(false);
                    mContentArea.append("Stop server succes!\r\n");//服务器成功停止
                    JOptionPane.showMessageDialog(mJFrame, "Stop server succes!");//服务器成功停止
                } catch (Exception exc) {
                    errorCallback("Stop the server an exception occurs!",JOptionPane.ERROR_MESSAGE);//停止服务器发生异常
                }
            }
        });
        mLANLogDebug = new LANLogDebugService(this);
    }

    @Override
    public void callbackLog(String logs) {
        mContentArea.append(logs);
    }

    @Override
    public void callbackUser(String user, int type) {
        if (type == LANLogDebugService.USER_ADD){
            mListModel.addElement(user);
        } else if (type == LANLogDebugService.USER_REMOVE){
            mListModel.removeElement(user);
        } else if (type == LANLogDebugService.USER_REMOVE_All){
            mListModel.removeAllElements();
        }
    }

    @Override
    public void errorCallback(String error, int errorType) {
        JOptionPane.showMessageDialog(mJFrame, error, "error", JOptionPane.ERROR_MESSAGE);
    }

}
