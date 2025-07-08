package com.vinay.GDrive.service;

import com.vinay.GDrive.entity.FilesEntity;
import com.vinay.GDrive.repo.FilesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilesService {

    @Autowired
    private FilesRepo filesRepo;

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

//    @Value("${file.upload-dir}")
//    private String uploadDir;

    public String uploadFile(MultipartFile file, Long parentFolderId) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Store metadata
            FilesEntity fileEntity = new FilesEntity();
            fileEntity.setName(fileName);
            fileEntity.setPath(fileName); // S3 key
            fileEntity.setSize(file.getSize());
            fileEntity.setType("file");
            fileEntity.setParentFolderId(parentFolderId);
            fileEntity.setCreatedAt(LocalDateTime.now());

            filesRepo.save(fileEntity);

            return "Uploaded to S3!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Upload failed.";
        }
    }

    public List<FilesEntity> getAllFiles(Long parentFolderId){
        if(parentFolderId == null){
            return filesRepo.findAll()
                    .stream()
                    .filter(f->f.getParentFolderId() == null)
                    .collect(Collectors.toList());
        }
        else{
            return filesRepo.findAll()
                    .stream()
                    .filter(f->parentFolderId.equals(f.getParentFolderId()))
                    .collect(Collectors.toList());
        }
    }

    public FilesEntity getFileById(Long id){
        return filesRepo.findById(id).orElseThrow(()->new RuntimeException("File not found"));
    }

    public void deleteById(Long id){
        filesRepo.deleteById(id);
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }



}
