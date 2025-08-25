package com.get.zh_picture_backend.controller;

import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.get.zh_picture_backend.annotation.AuthCheck;
import com.get.zh_picture_backend.common.BaseResponse;
import com.get.zh_picture_backend.model.enums.ErrorCode;
import com.get.zh_picture_backend.constant.UserConstant;
import com.get.zh_picture_backend.exception.BusinessException;
import com.get.zh_picture_backend.manager.CosManager;
import com.get.zh_picture_backend.utils.ResultUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Api(tags="fileController")
@Slf4j
public class FileController {
    @Resource
    private CosManager cosManager;


    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart MultipartFile multipartFile) {
        log.info("上传文件:{}",multipartFile);
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        //重构一个filapath,在filename的前面再加一层/test
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 创建一个临时文件
            file = File.createTempFile(filepath, null);
            //将multipartFile的内容写到file中，因为putObject中需要的参数是FiLe类型的，因此必须由multipartFile转换为File类型
            multipartFile.transferTo(file);
            //randomKey就是在cos中存储的名称
            cosManager.putObject(generateRandomKey(), file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            //1.根据filepath从存储对象中获取到cosObject对象
            COSObject cosObject = cosManager.getObject(filepath);
            //2.获取对象的一个输入流
            cosObjectInput = cosObject.getObjectContent();
            //3. 将输入流写到字节数组中
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            //4.设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            //5.写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }
    public String generateRandomKey(){
        String s = RandomUtil.randomString(8);
        System.out.println(s);
        return s;
    }
}
