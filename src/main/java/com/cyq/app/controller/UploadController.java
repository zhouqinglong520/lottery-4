package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;

/**
 * @author Lee lu
 * @since 2019/3/21
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/test")
public class UploadController extends ApiController {

    @Autowired
    Environment env;

    /**
     * 目录层级为一级的单文件上传
     * 若目录层级比较深，则可以在FileProcessor的实现类手动指定目录层级
     *
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping("/{uploadpath}/upload")
    public R<Object> uploadOne(
            @NotNull @RequestParam("file") MultipartFile file,
            @PathVariable("uploadpath") String uploadpath) throws Exception {
        String res_server = env.getProperty("res.server");
        String res_root = env.getProperty("res.root");
        log.info("res.server = {}, res.root = {}", res_server, res_root);
        String originalFilename = file.getOriginalFilename();
        String fileSuffix = FilenameUtils.getExtension(originalFilename);
        String filename = FilenameUtils.getName(originalFilename);
        log.info("originalFilename = {}, filename = {}, fileSuffix = {}", originalFilename, filename, fileSuffix);
        // 文件存储路径
        String filepath = res_root + uploadpath;
        File dir = new File(filepath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        File targetFile = new File(dir, filename);
        file.transferTo(targetFile);
        String httpurl = res_server + "/" + filename;
        return success(httpurl);
    }
}
