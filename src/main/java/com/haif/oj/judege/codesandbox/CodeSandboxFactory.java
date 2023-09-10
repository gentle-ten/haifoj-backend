package com.haif.oj.judege.codesandbox;

import com.haif.oj.common.ErrorCode;
import com.haif.oj.judege.codesandbox.impl.ExampleCodeSandbox;
import com.haif.oj.judege.codesandbox.impl.RemoteCodeSandbox;
import com.haif.oj.judege.codesandbox.impl.ThirdPartyCodeSanbox;

/**
 * 代码沙箱工厂类（根据字符串参数创建指定的代码沙箱实例）
 */
public class CodeSandboxFactory {

    public static CodeSandbox newInstance(String type) {
        switch (type) {
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox();
            case "thirdParty":
                return new ThirdPartyCodeSanbox();
            default:
                throw new RuntimeException("输入错误" + ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

}
