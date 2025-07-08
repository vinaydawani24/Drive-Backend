package com.vinay.GDrive.controller;

import com.vinay.GDrive.entity.FilesEntity;
import com.vinay.GDrive.service.FilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin("*")
public class FilesController {

    @Autowired
    private FilesService filesService;

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(@RequestParam("file")MultipartFile file,
                                             @RequestParam(value = "parentFolderId",required = false) Long parentFolderId){
        try {
            String response = filesService.uploadFile(file, parentFolderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed!");
        }

    }

    @GetMapping("/List")
    public ResponseEntity<List<FilesEntity>> getAllFiles(@RequestParam(value = "parentFolderId",required = false) Long parentFolderId){
        List<FilesEntity> files;
        if(parentFolderId==null){
           files =  filesService.getAllFiles(null);
        }
        else{
           files =  filesService.getAllFiles(parentFolderId);
        }
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id){
        try {
            FilesEntity filesEntity = filesService.getFileById(id);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filesEntity.getPath())
                    .build());
            filesService.deleteById(id);
            return ResponseEntity.ok("File Deleted!");
        }
        catch (Exception e){
            return ResponseEntity.status(500).body("Fail to delete");
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long id) {
        try {
            FilesEntity file = filesService.getFileById(id);
            ResponseInputStream<GetObjectResponse> s3Stream = filesService.downloadFile(file.getPath());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.getSize())
                    .body(new InputStreamResource(s3Stream));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
