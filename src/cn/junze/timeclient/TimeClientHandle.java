package cn.junze.timeclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Administrator on 2018/5/13 0013.
 */
public class TimeClientHandle implements Runnable {

    private String host;

    private int port;

    private Selector selector;

    private SocketChannel socketChannel;

    private boolean stop;

    public TimeClientHandle(String host, int port) {
        this.host = host == null ? "127.0.0.1" : host;
        this.port = port;

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();

            System.exit(1);
        }
    }

    @Override
    public void run() {

        try {
            //尝试连接服务器，连接成功发送数据
            doConnect();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (!stop) {

            try {
                //设置超时时间为1秒
                selector.select(1000);

                //selector获取事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey key = null;


                while (iterator.hasNext()) {
                    //取到事件
                    key = iterator.next();
                    iterator.remove();
                    //处理事件
                    handleInput(key);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private void doConnect() throws IOException {
        //尝试连接服务器
        if (socketChannel.connect(new InetSocketAddress(host, port))) {
            //直接连接成功
            //向selector注册可读事件
            socketChannel.register(selector, SelectionKey.OP_READ);

            //向服务端发送数据
            doWrite(socketChannel);
        } else {
            //没有直接连接成功
            //向selector注册连接成功事件
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }


    //处理事件,和服务端类似
    private void handleInput(SelectionKey key) throws IOException {

        if (key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();
            //如果没有直接连接成功，异步连接成功时，会走这里
            if (key.isConnectable()) {
                if (sc.finishConnect()) {
                    System.out.println("连接成功");
                    //和直接连接成功时，处理一样的逻辑
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                } else {

                    System.exit(1);
                }

            }

            //处理服务器响应
            if (key.isReadable()) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int read = sc.read(byteBuffer);
                if (read > 0) {
                    byteBuffer.flip();
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    String body = new String(bytes, "utf-8");
                    System.out.println("接收到服务器的响应为：" + body);
                    //接收一次停止
                    //this.stop = true;
                } else if (read < 0) {
                    key.cancel();
                    sc.close();
                }
            }
        }
    }


    //向服务端写数据
    private void doWrite(SocketChannel sc) throws IOException {
        byte[] request = "Query Time Order".getBytes();

        ByteBuffer writeBuffer = ByteBuffer.allocate(request.length);

        writeBuffer.put(request);

        //这里少写了，不能接收数据，调试好久
        writeBuffer.flip();

        sc.write(writeBuffer);

        //判断是否一次发送完成，如果没有一次发送完成，还需处理半包写问题
        if (!writeBuffer.hasRemaining()) {
            //一次发送完成
            System.out.println("Send order to server succeed");
        }

    }
}
