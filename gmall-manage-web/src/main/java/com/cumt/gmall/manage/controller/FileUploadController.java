package com.cumt.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    //当前类必须在Spring容器中
    @Value("${fileServer.url}")
    private String fileUrl;

    @Reference
    private ManageService manageService;

    //http://localhost:8082/fileUpload
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {

        String imgUrl = fileUrl;

        if(file != null){

            String congFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(congFile);
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient storageClient = new StorageClient(trackerServer,null);
            // 本地文件
            //String orginalFilename="/Users/baixiang/Desktop/002.jpg";

            //获取文件的上传名称
            String originalFilename = file.getOriginalFilename();
            String extName = StringUtils.substringAfterLast(originalFilename, ".");
            System.out.println(extName);  //jpg

            //String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            System.out.println(upload_file.length); //2

            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                System.out.println("path = " + path);

                imgUrl += "/" + path;
            }

            //path = group1
            //path = M00/00/00/wKgMbl106nOAcsVAAAEm8gd2d74608.jpg
        }

        //http://192.168.12.110/group1/M00/00/00/wKgMbl106nOAcsVAAAEm8gd2d74608.jpg
        return imgUrl;
    }
}
