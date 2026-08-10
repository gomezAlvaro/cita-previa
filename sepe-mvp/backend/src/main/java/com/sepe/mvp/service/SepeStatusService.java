package com.sepe.mvp.service;

import com.sepe.mvp.model.AppointmentRequest;
import com.sepe.mvp.model.OfficeAvailability;
import com.sepe.mvp.model.ProvinceStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class SepeStatusService {

    private final HttpClient httpClient;
    
    private static final Map<String, String> POSTAL_CODE_TO_PROVINCE = new HashMap<>();
    
    static {
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("28%03d", i), "28");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("08%03d", i), "08");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("46%03d", i), "46");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("41%03d", i), "41");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("50%03d", i), "50");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("29%03d", i), "29");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("30%03d", i), "30");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("07%03d", i), "07");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("35%03d", i), "35");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("48%03d", i), "48");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("03%03d", i), "03");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("14%03d", i), "14");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("47%03d", i), "47");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("36%03d", i), "36");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("33%03d", i), "33");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("18%03d", i), "18");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("01%03d", i), "01");
        for (int i = 0; i <= 999; i++) POSTAL_CODE_TO_PROVINCE.put(String.format("38%03d", i), "38");
    }

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

    private static final Map<String, List<String>> NEARBY_PROVINCES = new HashMap<>();
    static {
        NEARBY_PROVINCES.put("28", List.of("45", "40", "19", "47"));
        NEARBY_PROVINCES.put("08", List.of("43", "17", "25"));
        NEARBY_PROVINCES.put("46", List.of("12", "03", "30"));
        NEARBY_PROVINCES.put("41", List.of("11", "14", "29"));
        NEARBY_PROVINCES.put("50", List.of("22", "44", "31"));
        NEARBY_PROVINCES.put("29", List.of("11", "18", "04"));
        NEARBY_PROVINCES.put("30", List.of("03", "12", "04"));
        NEARBY_PROVINCES.put("07", List.of());
        NEARBY_PROVINCES.put("35", List.of("38"));
        NEARBY_PROVINCES.put("48", List.of("20", "39"));
        NEARBY_PROVINCES.put("03", List.of("46", "12", "30"));
        NEARBY_PROVINCES.put("14", List.of("41", "11", "29"));
        NEARBY_PROVINCES.put("47", List.of("40", "28", "49"));
        NEARBY_PROVINCES.put("36", List.of("15", "27", "32"));
        NEARBY_PROVINCES.put("33", List.of("24", "39"));
        NEARBY_PROVINCES.put("38", List.of("35"));
        NEARBY_PROVINCES.put("01", List.of("48", "20"));
        NEARBY_PROVINCES.put("11", List.of("41", "14", "29"));
        NEARBY_PROVINCES.put("18", List.of("29", "04", "23"));
    }

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

    public List<OfficeAvailability> findAvailableOffices(AppointmentRequest request) {
        String postalCode = request.getPostalCode();
        String provinceCode = POSTAL_CODE_TO_PROVINCE.get(postalCode);
        
        if (provinceCode == null) {
            provinceCode = inferProvinceFromPostalCode(postalCode);
        }
        
        List<OfficeAvailability> results = new ArrayList<>();
        
        if (provinceCode != null) {
            String provinceName = getProvinceName(provinceCode);
            
            boolean localAvailable = checkProvinceStatus(provinceCode);
            OfficeAvailability localOffice = new OfficeAvailability(
                "Oficina Principal " + provinceName,
                provinceName,
                "Calle Principal, " + postalCode,
                localAvailable,
                "0 km",
                "https://sede.sepe.gob.es/portalSede/es/Personas/CitaPrevia"
            );
            results.add(localOffice);
            
            List<String> nearbyCodes = NEARBY_PROVINCES.getOrDefault(provinceCode, List.of());
            for (String nearbyCode : nearbyCodes) {
                boolean available = checkProvinceStatus(nearbyCode);
                if (available) {
                    OfficeAvailability nearby = new OfficeAvailability(
                        "Oficina " + getProvinceName(nearbyCode),
                        getProvinceName(nearbyCode),
                        "Centro Ciudad",
                        true,
                        calculateDistance(provinceCode, nearbyCode),
                        "https://sede.sepe.gob.es/portalSede/es/Personas/CitaPrevia"
                    );
                    results.add(nearby);
                }
            }
        } else {
            for (String province : PROVINCES) {
                String code = province.substring(0, 2);
                if (checkProvinceStatus(code)) {
                    results.add(new OfficeAvailability(
                        "Oficina " + province.substring(3),
                        province.substring(3),
                        "Varias ubicaciones",
                        true,
                        "-",
                        "https://sede.sepe.gob.es/portalSede/es/Personas/CitaPrevia"
                    ));
                }
            }
        }
        
        return results;
    }

    private String inferProvinceFromPostalCode(String postalCode) {
        if (postalCode.length() >= 2) {
            String prefix = postalCode.substring(0, 2);
            for (String province : PROVINCES) {
                if (province.startsWith(prefix + "-")) {
                    return prefix;
                }
            }
        }
        return null;
    }

    private String getProvinceName(String code) {
        for (String province : PROVINCES) {
            if (province.startsWith(code + "-")) {
                return province.substring(3);
            }
        }
        return "Desconocida";
    }

    private boolean checkProvinceStatus(String provinceCode) {
        String url = "https://sede.sepe.gob.es/portalSede";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(2))
                .header("User-Agent", "Mozilla/5.0")
                .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
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
            } else if (statusCode >= 500 || responseTime > 5000 || statusCode == 503 || statusCode == 504) {
                status = "BUSY";
            }
        } catch (Exception e) {
            responseTime = System.currentTimeMillis() - startTime;
        }
        
        return new ProvinceStatus(code, name, status, responseTime, System.currentTimeMillis(), bookingUrl);
    }

    private String calculateDistance(String from, String to) {
        int fromInt = Integer.parseInt(from);
        int toInt = Integer.parseInt(to);
        int diff = Math.abs(fromInt - toInt);
        if (diff < 10) return "5-15 km";
        if (diff < 20) return "15-30 km";
        if (diff < 30) return "30-50 km";
        return "50+ km";
    }
}
