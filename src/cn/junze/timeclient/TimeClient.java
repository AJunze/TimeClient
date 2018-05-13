package cn.junze.timeclient;

/**
 * Created by Administrator on 2018/5/13 0013.
 */
public class TimeClient {
    public static void main(String[] args) {

        int port = 18080;

        if(args != null && args.length > 0){


            port = Integer.valueOf(args[0]);
        }

        new Thread(new TimeClientHandle("127.0.0.1",port),"client01").start();

    }
}
