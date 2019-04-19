package com.nio.serlizable;

import java.io.Serializable;

/**
 * Created by i-feng on 2019/4/19.
 */
public class NettyMessage implements Serializable{


    private static final long serialVersionUID = -2922146838546388140L;
    private Header header; // 消息头
    private Object body; // 消息体

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString(){
        return "NettyMessage [header=" + header + "]";
    }

}
