package com.example.report_service.controller;

import com.example.report_service.model.Report;
import com.example.report_service.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/get-report/{userId}")
    public ResponseEntity<Report> getReport(@PathVariable String userId){
        try
        {
            return ResponseEntity.ok(reportService.getReport(userId));
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/generate-report")
    public ResponseEntity<Report> generateReport(){
        try
        {
            reportService.weeklyReportGeneration();
            return ResponseEntity.ok().build();
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
