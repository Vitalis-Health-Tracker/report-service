package com.example.report_service.repository;

import com.example.report_service.model.Report;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ReportRepo extends ReactiveMongoRepository<Report, String> {
    public Mono<Report> findByUserIdAndReportDateBetween(String userId, LocalDateTime prevDate, LocalDateTime reportDate);
}
