package com.las.backenduser.repository;

import com.las.backenduser.model.WhitelistApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhitelistApplicationRepository extends MongoRepository<WhitelistApplication, String> {

    List<WhitelistApplication> findByStatusOrderByCreateTimeDesc(String status);

    boolean existsByUserUuidAndServerAndStatus(String userUuid, String server, String status);
}

