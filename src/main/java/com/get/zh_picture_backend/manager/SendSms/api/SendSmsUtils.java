package com.get.zh_picture_backend.manager.SendSms.api;

import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class SendSmsUtils {
    @Value("${aliYunAi.cloud.access_key_id}")
    private String accessKeyId;

    @Value("${aliYunAi.cloud.access_key_secret}")
    private String accessKeySecret;

    @Value("${aliYunAi.cloud.sign_name}")
    private String signName;

    @Value("${aliYunAi.cloud.template_code}")
    private String templateCode;

    @Value("${aliYunAi.cloud.endpoint}")
    private String endpoint;
    public  Client createClient() throws Exception {
        Config config = new Config()
                // 配置 AccessKey ID，请确保代码运行环境设置了环境变量。
                .setAccessKeyId(accessKeyId)
                // 配置 AccessKey Secret，请确保代码运行环境设置了环境变量。
                .setAccessKeySecret(accessKeySecret);
                // System.getenv()方法表示获取系统环境变量，请配置环境变量后，在此填入环境变量名称，不要直接填入AccessKey信息。

        // 配置 Endpoint
        config.setEndpoint(endpoint);

        return new Client(config);
    }

    public  SendSmsResponse sendSmsCode(String phoneNumber,String randomCode){
        SendSmsResponse sendSmsResponse = null;
        try {
            // 初始化请求客户端
            Client client = createClient();
            //获取随机值
            String templateParam=String.format("{\"code\":\"%s\"}",randomCode);
            // 构造请求对象，请填入请求参数值
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParam);
            // 获取响应对象
            sendSmsResponse = client.sendSms(sendSmsRequest);
            SendSmsResponseBody body = sendSmsResponse.getBody();
            System.out.println("发送验证码返回的结果为:"+body.getMessage()+" 请求的响应码为:"+body.getCode());
        } catch (TeaException e) {
            System.out.println("Exception:"+e.getMessage());
        } catch(Exception e){
            throw new RuntimeException(e);
        }
        return sendSmsResponse;
    }
}
