package cn.dfusion.ai.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author LDF
 * @date 2020/6/18
 */
public class CallExternalSystemDelegate implements JavaDelegate {
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("TODO 完成其他服务任务。。。"
                + delegateExecution.getVariable("employee"));
        delegateExecution.setVariable("result", "早产");
    }
}