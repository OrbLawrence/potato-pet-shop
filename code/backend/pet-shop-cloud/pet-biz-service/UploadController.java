package com.petshop.biz.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.petshop.biz.config.OssConfig;
import com.petshop.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/upload")
@Slf4j
public class UploadController {

    @Autowired
    private OssConfig ossConfig;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
        String fileName = UUID.randomUUID() + extension;
        String objectKey = ossConfig.getUploadPrefix() + fileName;
        log.info("[BIZ] OSS 上传开始: originalName={}, size={} bytes, bucket={}, objectKey={}",
                originalFilename, file.getSize(), ossConfig.getBucketName(), objectKey);

        OSS ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
        try {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, file.getInputStream());
            log.info("[BIZ] OSS 上传成功: objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("[BIZ] OSS 上传失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }

        String imageUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint()
                .replace("http://", "")
                .replace("https://", "")
                + "/" + objectKey;
        log.info("[BIZ] OSS 文件访问地址生成: url={}", imageUrl);
        return Result.success(imageUrl);
    }
}
