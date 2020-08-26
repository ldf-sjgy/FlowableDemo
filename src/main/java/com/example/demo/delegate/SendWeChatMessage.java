package com.example.demo.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author LDF
 * @date 2020/6/20
 */
public class SendWeChatMessage implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("TODO 3个月后给患者发微信复查提醒通知。。。"
                + delegateExecution.getVariable("name"));
    }
}
