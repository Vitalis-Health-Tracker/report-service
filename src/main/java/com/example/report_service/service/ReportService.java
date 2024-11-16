package com.example.report_service.service;

import com.example.report_service.dto.DietDto;
import com.example.report_service.dto.FitnessDto;
import com.example.report_service.dto.UserDto;
import com.example.report_service.dto.WellbeingDto;
import com.example.report_service.model.Report;
import com.example.report_service.repository.ReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ReportService {

    @Autowired
    private ReportRepo reportRepo;

    WebClient webClient = WebClient.create();

    public Mono<Report> getReportByDate(LocalDateTime Date, String userId) {
        LocalDateTime prevDate = Date.minusDays(7);
        return reportRepo.findByUserIdAndReportDateBetween(userId, prevDate, Date);
    }

    private Mono<List<DietDto>> collectDietData(String userId, LocalDateTime startDate, LocalDateTime endDate)
    {
        return webClient.get()
                .uri("http://localhost:9093/diet/"+userId+ "/get-diet-week?startDate="+startDate+"&endDate="+endDate)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Something Went Wrong!!")))
                .bodyToFlux(DietDto.class)
                .collectList();
    }

    private Mono<List<FitnessDto>> collectFitnessData(String userId, LocalDateTime startDate, LocalDateTime endDate)
    {
        return webClient.get()
                .uri("http://localhost:9092/health/fitness/"+userId+ "/get-workouts?startDate="+startDate+"&endDate="+endDate)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Something Went Wrong!!")))
                .bodyToFlux(FitnessDto.class)
                .collectList();
    }

    private Mono<List<WellbeingDto>> collectWellbeingData(String userId, LocalDateTime startDate, LocalDateTime endDate)
    {
        return webClient.get()
                .uri("http://localhost:9094/wellbeing/date?userId="+userId+"&startDate="+startDate+"&endDate="+endDate)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Something Went Wrong!!")))
                .bodyToFlux(WellbeingDto.class)
                .collectList();
    }

    private Mono<UserDto> collectUserData(String userId)
    {
        return webClient.get()
                .uri("http://localhost:9091/"+userId+"/get-details")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Something Went Wrong!!")))
                .bodyToMono(UserDto.class);
    }

    private Mono<List<String>> collectAllUsers()
    {
        return webClient.get()
                .uri("http://localhost:9091/user/get-all-ids")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Something Went Wrong!!")))
                .bodyToFlux(String.class)
                .collectList();

    }

    public Mono<Report> generateReport(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        Float total = 0f;
        Float bmr;
        Mono<Report> newReport = Mono.zip(
                collectDietData(userId, startDate, endDate),
                collectFitnessData(userId, startDate, endDate),
                collectWellbeingData(userId, startDate, endDate),
                collectUserData(userId)
            ).flatMap(tuple -> {
                Report report = new Report();
                report.setUserId(userId);
                report.setReportDate(LocalDateTime.now());
                report.setDiet(tuple.getT1());
                report.setFitness(tuple.getT2());
                report.setWellbeing(tuple.getT3());
                report.setUser(tuple.getT4());
                return Mono.just(report);
            });
        for(DietDto dietDto : Objects.requireNonNull(newReport.block()).getDiet())
        {
            total = total + dietDto.getTotalCaloriesConsumed();
        }
        newReport.block().setFinalCaloriesConsumed(total);

        for(FitnessDto fitnessDto : Objects.requireNonNull(newReport.block()).getFitness())
        {
            total = total - fitnessDto.getTotalCaloriesBurned();
        }
        newReport.block().setFinalCaloriesBurned(total);

        if(Objects.requireNonNull(newReport.block()).getUser().getGender().equals("Male"))
        {
            bmr = (float) (((10 * Objects.requireNonNull(newReport.block()).getUser().getWeight()) + (6.25 * Objects.requireNonNull(newReport.block()).getUser().getHeight()) - (5 * Objects.requireNonNull(newReport.block()).getUser().getAge()) + 5)*7);
            newReport.block().setUserCalorie(bmr);
        }
        else
        {
            bmr = (float) (((10 * Objects.requireNonNull(newReport.block()).getUser().getWeight()) + (6.25 * Objects.requireNonNull(newReport.block()).getUser().getHeight()) - (5 * Objects.requireNonNull(newReport.block()).getUser().getAge()) - 161)*7);
            newReport.block().setUserCalorie(bmr);
        }

        if(Objects.requireNonNull(newReport.block()).getUser().getJourney().equals("WEIGHT_LOSS"))
        {
            if(Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() > (bmr-(bmr*0.25)))
            {
                newReport.block().setStatus("You are doing great!!!");
            }
            else if (Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() < (bmr-(bmr*0.25)))
            {
                newReport.block().setStatus("There is mild progress.. we need to try harder!!");
            }
            else
            {
                newReport.block().setStatus("We need to work harder!!!");
            }
        }
        else if(Objects.requireNonNull(newReport.block()).getUser().getJourney().equals("MAINTAIN"))
        {
            if(Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() == bmr)
            {
                newReport.block().setStatus("You are doing great!!!");
            }
            else if (Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() < bmr)
            {
                newReport.block().setStatus("You might be losing weight slightly... be careful");
            }
            else
            {
                newReport.block().setStatus("You may be gaining weight... be careful");
            }
        }
        else
        {
            if(Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() > (bmr+(bmr*0.25)))
            {
                newReport.block().setStatus("You are doing great!!!");
            }
            else if (Objects.requireNonNull(newReport.block()).getFinalCaloriesConsumed() < (bmr+(bmr*0.25)))
            {
                newReport.block().setStatus("There is mild progress.. we need to try harder!!");
            }
            else
            {
                newReport.block().setStatus("We need to work harder!!!");
            }
        }

        return reportRepo.save(newReport.block());
    }

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Kolkata")
    public void weeklyReportGeneration() {
        Mono<List<String>> userIds = collectAllUsers();
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        for(String userId : Objects.requireNonNull(userIds.block()))
        {
            generateReport(userId, startDate, endDate);
        }
    }

    public Report getReport(String userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().minusDays(0);
        Mono<Report> report = getReportByDate(LocalDateTime.now(), userId);
        if(report.block() == null)
        {
            throw new RuntimeException("Report not found");
        }
        else
        {
            return report.block();
        }
    }
}
