package com.example.report_service.service;

import com.example.report_service.dto.DietDto;
import com.example.report_service.dto.FitnessDto;
import com.example.report_service.dto.UserDto;
import com.example.report_service.dto.WellbeingDto;
import com.example.report_service.model.Report;
import com.example.report_service.repository.ReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.core.ParameterizedTypeReference;
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
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Diet Went Wrong!!")))
                .bodyToFlux(DietDto.class)
                .collectList();
    }

    private Mono<List<FitnessDto>> collectFitnessData(String userId, LocalDateTime startDate, LocalDateTime endDate)
    {
        return webClient.get()
                .uri("http://localhost:9092/health/fitness/"+userId+ "/get-workouts?startDate="+startDate+"&endDate="+endDate)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Fitness Went Wrong!!")))
                .bodyToFlux(FitnessDto.class)
                .collectList();
    }

    private Mono<List<WellbeingDto>> collectWellbeingData(String userId, LocalDateTime startDate, LocalDateTime endDate)
    {
        return webClient.get()
                .uri("http://localhost:9094/wellbeing/date?userId="+userId+"&startDate="+startDate+"&endDate="+endDate)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Wellbeing Went Wrong!!")))
                .bodyToFlux(WellbeingDto.class)
                .collectList();
    }

    private Mono<UserDto> collectUserData(String userId)
    {
        return webClient.get()
                .uri("http://localhost:9091/user/"+userId+"/get-details")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("User Went Wrong!!")))
                .bodyToMono(UserDto.class);
    }

    private Mono<List<String>> collectAllUsers()
    {
        return webClient.get()
                .uri("http://localhost:9091/user/get-all-ids")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("UserIds Went Wrong!!")))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});
                //.collectList();

    }

    public Mono<Report> generateReport(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return Mono.zip(
                        collectDietData(userId, startDate, endDate),  // List<DietDto>
                        collectFitnessData(userId, startDate, endDate),  // List<FitnessDto>
                        collectWellbeingData(userId, startDate, endDate),  // List<WellbeingDto>
                        collectUserData(userId)  // UserDto
                )
                .map(tuple -> {
                    // Get the lists from the tuple
                    List<DietDto> diet = tuple.getT1();
                    List<FitnessDto> fitness = tuple.getT2();
                    List<WellbeingDto> wellbeing = tuple.getT3();
                    UserDto user = tuple.getT4();

                    // Calculate the total calories consumed from the diet list
                    Float totalCaloriesConsumed = diet.stream()
                            .map(DietDto::getTotalCaloriesConsumed)
                            .reduce(0f, Float::sum);  // Sum of all consumed calories

                    // Calculate the total calories burned from the fitness list
                    Float totalCaloriesBurned = fitness.stream()
                            .map(FitnessDto::getTotalCaloriesBurned)
                            .reduce(0f, Float::sum);  // Sum of all burned calories

                    // Calculate BMR based on user's gender
                    Float bmr;
                    if ("Male".equals(user.getGender())) {
                        bmr = (float) (((10 * user.getWeight()) + (6.25 * user.getHeight()) - (5 * user.getAge()) + 5));
                    } else {
                        bmr = (float) (((10 * user.getWeight()) + (6.25 * user.getHeight()) - (5 * user.getAge()) - 161));
                    }

                    // Build the report object
                    Report report = new Report();
                    report.setUserId(userId);
                    report.setReportDate(LocalDateTime.now());
                    report.setDiet(diet);  // Assigning the list of DietDto
                    report.setFitness(fitness);  // Assigning the list of FitnessDto
                    report.setWellbeing(wellbeing);  // Assigning the list of WellbeingDto
                    report.setUser(user);  // Assigning the UserDto
                    report.setFinalCaloriesConsumed(totalCaloriesConsumed);  // Set the total calories consumed
                    report.setFinalCaloriesBurned(totalCaloriesBurned);  // Set the total calories burned
                    report.setUserCalorie(bmr);  // Set the calculated BMR

                    // Set Status based on journey type and calories
                    if ("WEIGHT_LOSS".equals(user.getJourney())) {
                        if (totalCaloriesConsumed > (bmr - (bmr * 0.25))) {
                            report.setStatus("You are doing great!!!");
                        } else if (totalCaloriesConsumed < (bmr - (bmr * 0.25))) {
                            report.setStatus("There is mild progress.. we need to try harder!!");
                        } else {
                            report.setStatus("We need to work harder!!!");
                        }
                    } else if ("MAINTAIN".equals(user.getJourney())) {
                        if (totalCaloriesConsumed == bmr) {
                            report.setStatus("You are doing great!!!");
                        } else if (totalCaloriesConsumed < bmr) {
                            report.setStatus("You might be losing weight slightly... be careful");
                        } else {
                            report.setStatus("You may be gaining weight... be careful");
                        }
                    } else {
                        if (totalCaloriesConsumed > (bmr + (bmr * 0.25))) {
                            report.setStatus("You are doing great!!!");
                        } else if (totalCaloriesConsumed < (bmr + (bmr * 0.25))) {
                            report.setStatus("There is mild progress.. we need to try harder!!");
                        } else {
                            report.setStatus("We need to work harder!!!");
                        }
                    }
                    return report;
                })
                .flatMap(reportRepo::save);
    }

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Kolkata")
    public void weeklyReportGeneration() {
        try {
            Mono<List<String>> userIds = collectAllUsers();
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            System.out.println("mono userIds : " + userIds);

            userIds.flatMapMany(Flux::fromIterable)
                    .flatMap(userId -> {
                        return generateReport(userId, startDate, endDate);
                    })
                    .doOnError(e -> System.out.println("Error generating report: " + e.getMessage()))
                    .subscribe();

        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

//    public Report getReport(String userId) {
//        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
//        LocalDateTime endDate = LocalDateTime.now().minusDays(0);
//        Mono<Report> report = getReportByDate(LocalDateTime.now(), userId);
//        if(report.block() == null)
//        {
//            throw new RuntimeException("Report not found");
//        }
//        else
//        {
//            return report.block();
//        }
//    }
}
