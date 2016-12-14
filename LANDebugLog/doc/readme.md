1、javalib为java library的Module
    1.1、LANLogDebugClientJFrame为swt可视界面，LANLogDebugClient为该界面剥离出来的逻辑类，可考走在其他地方直接使用
    1.2、LANLogServiceSocketJFrame 和 LANLogDebugService同上

2、app中为Android 工程，里面集成LANLogDebugClient，与javalib中的LANLogDebugClient其实也是应该保持一致的

3、jarfiles里面为客户端与服务端的swt的调试jar包，可直接双击使用