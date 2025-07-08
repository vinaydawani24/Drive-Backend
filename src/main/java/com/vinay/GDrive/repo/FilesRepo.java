package com.vinay.GDrive.repo;

import com.vinay.GDrive.entity.FilesEntity;
import com.vinay.GDrive.entity.FilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepo extends JpaRepository<FilesEntity,Long> {
}
