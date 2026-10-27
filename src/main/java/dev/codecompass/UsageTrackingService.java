package dev.codecompass;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageTrackingService {
    private final UsageEventRepository usageRepository;
    private final AppUserRepository userRepository;

    public UsageTrackingService(UsageEventRepository usageRepository, AppUserRepository userRepository) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordUsage(String userId, String source, String modelName, String status,
                            int promptTokens, int completionTokens) {
        userRepository.findById(userId).ifPresent(user -> {
            UsageEvent event = new UsageEvent(
                    user,
                    source != null ? source : "WEB",
                    modelName != null ? modelName : "gemini-2.5-flash",
                    status != null ? status : "SUCCESS",
                    Math.max(0, promptTokens),
                    Math.max(0, completionTokens)
            );
            usageRepository.save(event);
        });
    }

    @Transactional(readOnly = true)
    public SettingsDtos.UsageSummaryDto getUsageSummary(String userId) {
        long totalRequests = usageRepository.countByUser_Id(userId);
        long successfulRequests = usageRepository.countByUser_IdAndStatus(userId, "SUCCESS");
        long failedRequests = usageRepository.countByUser_IdAndStatus(userId, "FAILED");
        long webRequests = usageRepository.countByUser_IdAndSource(userId, "WEB");
        long apiKeyRequests = usageRepository.countByUser_IdAndSource(userId, "API_KEY");

        List<UsageEvent> recentList = usageRepository.findTop50ByUser_IdOrderByCreatedAtDesc(userId);
        long estimatedTokens = recentList.stream().mapToLong(UsageEvent::getTotalTokens).sum();

        // 7-day daily breakdown
        LocalDateTime sevenDaysAgo = LocalDateTime.now(Clock.systemUTC()).minusDays(7);
        List<UsageEvent> weekEvents = usageRepository.findAllByUser_IdAndCreatedAtAfter(userId, sevenDaysAgo);

        Map<String, long[]> dailyMap = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            String dayKey = LocalDate.now(Clock.systemUTC()).minusDays(i).format(fmt);
            dailyMap.put(dayKey, new long[]{0, 0}); // [requests, tokens]
        }

        for (UsageEvent ev : weekEvents) {
            String dayKey = ev.getCreatedAt().toLocalDate().format(fmt);
            if (dailyMap.containsKey(dayKey)) {
                long[] stats = dailyMap.get(dayKey);
                stats[0]++;
                stats[1] += ev.getTotalTokens();
            }
        }

        List<SettingsDtos.DailyUsageDto> dailyList = new ArrayList<>();
        dailyMap.forEach((day, stats) -> dailyList.add(new SettingsDtos.DailyUsageDto(day, stats[0], stats[1])));

        List<SettingsDtos.RecentUsageDto> recentDtos = recentList.stream()
                .map(SettingsDtos.RecentUsageDto::from)
                .toList();

        return new SettingsDtos.UsageSummaryDto(
                totalRequests,
                successfulRequests,
                failedRequests,
                estimatedTokens,
                webRequests,
                apiKeyRequests,
                dailyList,
                recentDtos
        );
    }
}
