package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.swagger.annotations.Api;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Api(tags = "文件上传")
@RestController
@RequestMapping("/admin/product")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileServerUrl; // 我们使用软配置的方式——将配置信息写在配置文件中获取

    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws MyException, IOException { // 名字一定要file因为前端是传这个名字
        // 1. 加载配置文件tracker.conf
        String configFile = Objects.requireNonNull(this.getClass().getResource("/tracker.conf")).getFile();
        String path = "";
        if (Objects.nonNull(configFile)) {
            // 2. 初始化当前文件
            ClientGlobal.init(configFile);
            // 3. 创建 TrackerServer
            TrackerServer trackerServer = new TrackerClient().getConnection();
            // 4. 创建 StorageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 5. 文件上传
            String extName = FilenameUtils.getExtension(file.getOriginalFilename()); //文件扩展名
            path = storageClient1.upload_appender_file1(file.getBytes(), extName, null);
        }
        return Result.ok(fileServerUrl + path);
    }

}
