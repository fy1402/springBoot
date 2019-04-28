package com.nio.serlizable;

public interface MessageType {

    Byte BUSINESS_REQ = 0;  // 业务请求

    Byte BUSINESS_RESP = 1; // 业务返回

    Byte BUSINESS_ONE_WAY = 2; //2: 业务ONE WAY消息(即是请求又是响应消息)

    Byte LOGIN_REQ = 3;  // 握手请求

    Byte LOGIN_RESP = 4; // 握手返回

    Byte HEARTBEAT_REQ = 5; // 心跳请求

    Byte HEARTBEAT_RESP = 6; // 心跳应答
}
