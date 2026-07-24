package com.shorepower.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final SystemConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile double baseTemperature = 30.0;
    private volatile long lastFetchTime = 0;
    private static final long FETCH_INTERVAL = 5 * 60 * 1000L; // 5分钟

    @PostConstruct
    public void init() {
        fetchTemperature();
    }

    public void refreshNow() {
        fetchTemperature();
    }

    public void refreshNow(String location) {
        fetchTemperature(location);
    }

    public double getCurrentAmbient() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > FETCH_INTERVAL) {
            fetchTemperature();
        }
        int hour = LocalTime.now().getHour();
        double dailyVariation = 5.0 * Math.sin((hour - 14) * 2.0 * Math.PI / 24.0);
        return baseTemperature + dailyVariation + (Math.random() - 0.5) * 2.0;
    }

    private void fetchTemperature() {
        String location = configService.getConfigValue("weather.location");
        if (location == null || location.isEmpty()) location = "shanghai";
        fetchTemperature(location);
    }

    private void fetchTemperature(String location) {
        try {
            String apiKey = configService.getConfigValue("weather.api.key");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("weather.api.key 未配置");
            }

            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q="
                    + encodedLocation + "&appid=" + apiKey + "&units=metric");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonNode root = objectMapper.readTree(response.toString());
            JsonNode tempNode = root.path("main").path("temp");
            if (!tempNode.isMissingNode()) {
                baseTemperature = tempNode.asDouble();
                lastFetchTime = System.currentTimeMillis();
                log.info("天气API更新成功: {} = {}°C", location, String.format("%.1f", baseTemperature));
            }
        } catch (Exception e) {
            int fallback = configService.getIntConfig("temperature.ambient.base", 30);
            baseTemperature = fallback;
            log.warn("天气API获取失败: {}, 使用兜底值: {}°C", e.getMessage(), fallback);
        }
    }
}
