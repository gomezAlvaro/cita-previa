package com.sepe.mvp.service;

import com.sepe.mvp.model.PortalCheck;
import com.sepe.mvp.model.PortalStatus;
import com.sepe.mvp.model.ProvinceStatus;
import com.sepe.mvp.model.SearchResult;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks the state of the SEPE cita previa portal and maps postal codes to provinces.
 *
 * SEPE serves all provinces from one portal, so the portal is probed once and the
 * result is shared. The probe result is cached for {@link #CACHE_TTL_MS}, which also
 * caps how often this app can hit SEPE (at most one request per minute), no matter
 * how many users call the API.
 */
@Service
public class SepeStatusService {

    static final String PORTAL_URL = "https://sede.sepe.gob.es/portalSede";
    static final String BOOKING_URL = "https://sede.sepe.gob.es/portalSede/es/Personas/CitaPrevia";
    static final long CACHE_TTL_MS = 60_000;
    private static final long SLOW_THRESHOLD_MS = 2_000;

    /** INE province codes, in code order. */
    private static final Map<String, String> PROVINCES = new LinkedHashMap<>();
    /** Neighbouring provinces (by code) worth suggesting if the user's own one is busy. */
    private static final Map<String, List<String>> NEARBY_PROVINCES = new LinkedHashMap<>();

    static {
        String[] names = {
            "01=Álava", "02=Albacete", "03=Alicante", "04=Almería", "05=Ávila",
            "06=Badajoz", "07=Baleares", "08=Barcelona", "09=Burgos", "10=Cáceres",
            "11=Cádiz", "12=Castellón", "13=Ciudad Real", "14=Córdoba", "15=A Coruña",
            "16=Cuenca", "17=Girona", "18=Granada", "19=Guadalajara", "20=Gipuzkoa",
            "21=Huelva", "22=Huesca", "23=Jaén", "24=León", "25=Lleida",
            "26=La Rioja", "27=Lugo", "28=Madrid", "29=Málaga", "30=Murcia",
            "31=Navarra", "32=Ourense", "33=Asturias", "34=Palencia", "35=Las Palmas",
            "36=Pontevedra", "37=Salamanca", "38=Santa Cruz de Tenerife", "39=Cantabria", "40=Segovia",
            "41=Sevilla", "42=Soria", "43=Tarragona", "44=Teruel", "45=Toledo",
            "46=Valencia", "47=Valladolid", "48=Bizkaia", "49=Zamora", "50=Zaragoza",
            "51=Ceuta", "52=Melilla"
        };
        for (String entry : names) {
            String[] parts = entry.split("=", 2);
            PROVINCES.put(parts[0], parts[1]);
        }

        neighbours("01", "48,20,31,26,09");
        neighbours("02", "16,13,46,03,30,23");
        neighbours("03", "46,30,02");
        neighbours("04", "18,30,23");
        neighbours("05", "40,28,45,37,47");
        neighbours("06", "10,41,21,14,13");
        neighbours("07", "");
        neighbours("08", "43,17,25");
        neighbours("09", "34,39,01,26,42,40,47,48");
        neighbours("10", "06,37,05,45");
        neighbours("11", "41,29,21");
        neighbours("12", "46,43,44");
        neighbours("13", "45,02,23,14,06,16");
        neighbours("14", "41,29,18,23,13,06");
        neighbours("15", "36,27");
        neighbours("16", "19,02,46,13,45");
        neighbours("17", "08");
        neighbours("18", "29,23,04,14");
        neighbours("19", "28,16,42,40");
        neighbours("20", "48,01,31");
        neighbours("21", "41,06,11");
        neighbours("22", "50,25,31");
        neighbours("23", "13,14,18,02");
        neighbours("24", "33,34,47,49,27,32");
        neighbours("25", "08,43,22");
        neighbours("26", "01,31,09,42,50");
        neighbours("27", "15,33,24,32,36");
        neighbours("28", "45,40,19,05,16");
        neighbours("29", "11,41,14,18");
        neighbours("30", "03,02,04,18");
        neighbours("31", "01,20,22,50,26");
        neighbours("32", "36,27,24,49");
        neighbours("33", "39,24,27");
        neighbours("34", "09,39,24,47");
        neighbours("35", "38");
        neighbours("36", "15,27,32");
        neighbours("37", "10,05,49,47");
        neighbours("38", "35");
        neighbours("39", "33,34,09,48");
        neighbours("40", "05,28,47,09,19");
        neighbours("41", "11,14,21,29,06");
        neighbours("42", "09,26,50,19,40");
        neighbours("43", "08,25,12,44");
        neighbours("44", "50,43,12,46,16,19");
        neighbours("45", "28,13,10,05,06,16");
        neighbours("46", "12,03,44,16,02");
        neighbours("47", "40,34,49,05,37,09,24");
        neighbours("48", "20,01,39");
        neighbours("49", "47,37,24,32");
        neighbours("50", "22,44,42,31,26");
        neighbours("51", "");
        neighbours("52", "");
    }

    private static void neighbours(String code, String csv) {
        NEARBY_PROVINCES.put(code, csv.isEmpty() ? List.of() : Arrays.asList(csv.split(",")));
    }

    private final HttpClient httpClient;
    private PortalCheck cachedCheck;

    public SepeStatusService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /** Status of every province. All share the same value because SEPE has a single portal. */
    public List<ProvinceStatus> getAllProvincesStatus() {
        PortalCheck check = checkPortal();
        List<ProvinceStatus> statuses = new ArrayList<>();
        for (Map.Entry<String, String> province : PROVINCES.entrySet()) {
            statuses.add(new ProvinceStatus(
                province.getKey(),
                province.getValue(),
                check.status().name(),
                check.responseTimeMs(),
                check.timestamp(),
                BOOKING_URL));
        }
        return statuses;
    }

    /**
     * Provinces to try for a postal code: its own province first, then its neighbours.
     *
     * @throws IllegalArgumentException if the postal code is not 5 digits or has no known province
     */
    public List<SearchResult> findProvincesForPostalCode(String postalCode) {
        String provinceCode = provinceCodeForPostalCode(postalCode);
        PortalStatus status = checkPortal().status();

        List<SearchResult> results = new ArrayList<>();
        results.add(new SearchResult(provinceCode, PROVINCES.get(provinceCode), false, status, BOOKING_URL));
        for (String nearbyCode : NEARBY_PROVINCES.getOrDefault(provinceCode, List.of())) {
            results.add(new SearchResult(nearbyCode, PROVINCES.get(nearbyCode), true, status, BOOKING_URL));
        }
        return results;
    }

    /** Province code (first two digits) for a Spanish postal code. */
    static String provinceCodeForPostalCode(String postalCode) {
        if (postalCode == null || !postalCode.matches("\\d{5}")) {
            throw new IllegalArgumentException("El código postal debe tener 5 dígitos");
        }
        String code = postalCode.substring(0, 2);
        if (!PROVINCES.containsKey(code)) {
            throw new IllegalArgumentException("Código postal desconocido: " + postalCode);
        }
        return code;
    }

    /** Probes the portal, at most once per {@link #CACHE_TTL_MS}. */
    synchronized PortalCheck checkPortal() {
        long now = System.currentTimeMillis();
        if (cachedCheck == null || now - cachedCheck.timestamp() >= CACHE_TTL_MS) {
            cachedCheck = probePortal();
        }
        return cachedCheck;
    }

    /** Performs one real request to the SEPE portal. Overridden in tests. */
    PortalCheck probePortal() {
        long start = System.currentTimeMillis();
        PortalStatus status;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PORTAL_URL))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0")
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsed = System.currentTimeMillis() - start;
            status = classify(response.statusCode(), elapsed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = PortalStatus.UNREACHABLE;
        } catch (Exception e) {
            status = PortalStatus.UNREACHABLE;
        }
        long end = System.currentTimeMillis();
        return new PortalCheck(status, end - start, end);
    }

    static PortalStatus classify(int httpStatus, long elapsedMs) {
        if (httpStatus == 403 || httpStatus == 429) {
            return PortalStatus.BLOCKED;
        }
        if (httpStatus >= 200 && httpStatus < 400) {
            return elapsedMs < SLOW_THRESHOLD_MS ? PortalStatus.OK : PortalStatus.SLOW;
        }
        return PortalStatus.DOWN;
    }
}
