package com.sepe.mvp.service;

import com.sepe.mvp.model.PortalCheck;
import com.sepe.mvp.model.PortalStatus;
import com.sepe.mvp.model.ProvinceStatus;
import com.sepe.mvp.model.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SepeStatusServiceTest {

    /** Service whose portal probe is faked, so tests never touch the network. */
    static class FakeService extends SepeStatusService {
        final AtomicInteger probes = new AtomicInteger();
        final PortalStatus status;

        FakeService(PortalStatus status) {
            this.status = status;
        }

        @Override
        PortalCheck probePortal() {
            probes.incrementAndGet();
            return new PortalCheck(status, 10, System.currentTimeMillis());
        }
    }

    @Test
    void mapsPostalCodesToTheRightProvince() {
        assertEquals("28", SepeStatusService.provinceCodeForPostalCode("28001"));
        assertEquals("48", SepeStatusService.provinceCodeForPostalCode("48001"));
        assertEquals("49", SepeStatusService.provinceCodeForPostalCode("49001"));
        assertEquals("50", SepeStatusService.provinceCodeForPostalCode("50001"));
        assertEquals("52", SepeStatusService.provinceCodeForPostalCode("52001"));
    }

    @Test
    void rejectsInvalidPostalCodes() {
        for (String bad : new String[] {null, "", "2800", "280011", "abcde", "00001", "99999"}) {
            assertThrows(IllegalArgumentException.class,
                () -> SepeStatusService.provinceCodeForPostalCode(bad), "should reject: " + bad);
        }
    }

    @Test
    void zaragozaAndBizkaiaHaveCorrectNames() {
        FakeService service = new FakeService(PortalStatus.OK);
        assertEquals("Zaragoza", service.findProvincesForPostalCode("50001").get(0).provinceName());
        assertEquals("Bizkaia", service.findProvincesForPostalCode("48001").get(0).provinceName());
        assertEquals("Zamora", service.findProvincesForPostalCode("49001").get(0).provinceName());
    }

    @Test
    void ownProvinceComesFirstThenNeighbours() {
        FakeService service = new FakeService(PortalStatus.OK);
        List<SearchResult> results = service.findProvincesForPostalCode("28001");

        assertEquals("28", results.get(0).provinceCode());
        assertFalse(results.get(0).nearby());
        assertTrue(results.size() > 1);
        assertTrue(results.stream().skip(1).allMatch(SearchResult::nearby));
        assertTrue(results.stream().noneMatch(r -> r.provinceName() == null));
    }

    @Test
    void provincesWithoutNeighboursReturnOnlyThemselves() {
        FakeService service = new FakeService(PortalStatus.OK);
        assertEquals(1, service.findProvincesForPostalCode("07001").size());
    }

    @Test
    void allFiftyTwoProvincesAreReported() {
        FakeService service = new FakeService(PortalStatus.SLOW);
        List<ProvinceStatus> statuses = service.getAllProvincesStatus();

        assertEquals(52, statuses.size());
        assertTrue(statuses.stream().allMatch(s -> s.getStatus().equals("SLOW")));
    }

    @Test
    void portalIsProbedOncePerCacheWindow() {
        FakeService service = new FakeService(PortalStatus.OK);
        service.getAllProvincesStatus();
        service.findProvincesForPostalCode("28001");
        service.findProvincesForPostalCode("08001");

        assertEquals(1, service.probes.get());
    }

    @Test
    void classifiesHttpResponses() {
        assertEquals(PortalStatus.OK, SepeStatusService.classify(200, 300));
        assertEquals(PortalStatus.SLOW, SepeStatusService.classify(200, 3000));
        assertEquals(PortalStatus.BLOCKED, SepeStatusService.classify(403, 100));
        assertEquals(PortalStatus.BLOCKED, SepeStatusService.classify(429, 100));
        assertEquals(PortalStatus.DOWN, SepeStatusService.classify(503, 100));
        assertEquals(PortalStatus.DOWN, SepeStatusService.classify(404, 100));
    }
}
