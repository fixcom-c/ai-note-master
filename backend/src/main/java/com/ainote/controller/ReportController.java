package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.service.DailyReport;
import com.ainote.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    public Result<DailyReport> getDaily(@RequestParam(required = false) String date) {
        LocalDate targetDate = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return Result.success(reportService.generateDailyReport(targetDate));
    }

    @PostMapping("/daily")
    public Result<DailyReport> generateDaily() {
        return Result.success(reportService.generateDailyReport(LocalDate.now()));
    }
}
