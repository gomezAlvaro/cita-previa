package com.sepe.mvp.service;

import com.sepe.mvp.model.ProvinceStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class SepeStatusService {

    private final HttpClient httpClient;
    
    // SEPE provincial URLs (using main sede electronica with province codes)
    private static final String[] PROVINCES = {
        "01-Alava", "02-Albacete", "03-Alicante", "04-Almeria", "05-Avila",
        "06-Badajoz", "07-Baleares", "08-Barcelona", "09-Burgos", "10-Caceres",
        "11-Cadiz", "12-Castellon", "13-Ciudad Real", "14-Cordoba", "15-Coruna",
        "16-Cuenca", "17-Girona", "18-Granada", "19-Guadalajara", "20-Gipuzkoa",
        "21-Huelva", "22-Huesca", "23-Jaen", "24-Leon", "25-Lleida", "26-Rioja",
        "27-Lugo", "28-Madrid", "29-Malaga", "30-Murcia", "31-Navarra", "32-Ourense",
        "33-Asturias", "34-Palencia", "35-Las Palmas", "36-Pontevedra", "37-Salamanca",
        "38-Santa Cruz de Tenerife", "39-Cantabria", "40-Segovia", "41-Sevilla",
        "42-Soria", "43-Tarragona", "44-Teruel", "45-Toledo", "46-Valencia",
        "47-Valladolid", "48-Zamora", "49-Zaragoza", "51-Ceuta", "52-Melilla"
    };

    public SepeStatusService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    }

    @Cacheable(value = "provinceStatuses")
    public List<ProvinceStatus> getAllProvincesStatus() {
        List<ProvinceStatus> statuses = new ArrayList<>();
        
        for (String province : PROVINCES) {
            statuses.add(checkProvince(province));
        }
        
        return statuses;
    }

    private ProvinceStatus checkProvince(String province) {
        String code = province.substring(0, 2);
        String name = province.substring(3);
        String url = "https://sede.sepe.gob.es/portalSede";
        String bookingUrl = "https://sede.sepe.gob.es/portalSede/es/Personas/CitaPrevia";
        
        long startTime = System.currentTimeMillis();
        String status = "UNKNOWN";
        long responseTime = 0;
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(3))
                .header("User-Agent", "Mozilla/5.0")
                .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            responseTime = System.currentTimeMillis() - startTime;
            
            int statusCode = response.statusCode();
            
            if (statusCode == 200 && responseTime < 2000) {
                status = "AVAILABLE";
            } else if (statusCode >= 500 || responseTime > 5000) {
                status = "BUSY";
            } else if (statusCode == 503 || statusCode == 504) {
                status = "BUSY";
            } else {
                status = "UNKNOWN";
            }
            
        } catch (Exception e) {
            responseTime = System.currentTimeMillis() - startTime;
            status = "UNKNOWN";
        }
        
        return new ProvinceStatus(code, name, status, responseTime, System.currentTimeMillis(), bookingUrl);
    }
}
