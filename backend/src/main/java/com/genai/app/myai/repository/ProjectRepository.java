package com.genai.app.myai.repository;

import com.genai.app.myai.repository.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    Optional<ProjectEntity> findByProjectIdAndSysCreateUser(Long projectId, String sysCreateUser);

    Page<ProjectEntity> findAllBySysCreateUser(String sysCreateUser, Pageable pageable);

    Page<ProjectEntity> findAllBySysCreateUserAndProjectNameLike(String sysCreateUser, String projectName, Pageable pageable);
}
