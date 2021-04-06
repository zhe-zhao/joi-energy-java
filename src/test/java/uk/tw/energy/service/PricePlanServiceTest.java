package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.generator.ElectricityReadingsGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.initMocks;

class PricePlanServiceTest {
    public static final String METER_ID_0 = "meter_0";
    public static final String METER_ID_1 = "meter_1";
    public static final String PLAN_ID_0 = "plan_id_0";
    public static final String PLAN_ID_1 = "plan_id_1";
    private final ElectricityReadingsGenerator generator = new ElectricityReadingsGenerator();

    private PricePlanService pricePlanService;
    @Mock
    private MeterReadingService meterReadingService;


    @BeforeEach
    public void setup() {
        initMocks(this);
        final List<PricePlan> pricePlans = List.of(
                new PricePlan(PLAN_ID_0, "Supplier 0", BigDecimal.ONE, emptyList()),
                new PricePlan(PLAN_ID_1, "Supplier 1", BigDecimal.TEN, emptyList())
        );

        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
    }

    @Test
    public void givenMeterId_shouldGetConsumptionCost() {
        // Time elapsed 360 seconds
        Mockito.when(meterReadingService.getReadings(any(String.class))).thenReturn(Optional.of(generator.generate(37, 1)));

        Map<String, BigDecimal> costMap = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(METER_ID_0).get();

        assertEquals(BigDecimal.valueOf(0.1).stripTrailingZeros(), costMap.get(PLAN_ID_0).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(1).stripTrailingZeros(), costMap.get(PLAN_ID_1).stripTrailingZeros());
        assertTrue(costMap.get(PLAN_ID_0).compareTo(BigDecimal.valueOf(0.1)) == 0);
        assertTrue(costMap.get(PLAN_ID_1).compareTo(BigDecimal.valueOf(1)) == 0);
    }

    @Test
    public void givenMeterId_shouldGetConsumptionCostForLongerPeriod() {
        // Time elapsed 14 days, 337 - 1 hours
        Mockito.when(meterReadingService.getReadings(any(String.class))).thenReturn(Optional.of(generator.generate(337, 1, 3600)));

        Map<String, BigDecimal> costMap = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(METER_ID_0).get();

        assertEquals(BigDecimal.valueOf(336).stripTrailingZeros(), costMap.get(PLAN_ID_0).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(3360).stripTrailingZeros(), costMap.get(PLAN_ID_1).stripTrailingZeros());
    }

    @Test
    public void givenMeterId_shouldGetConsumptionCostForPrevNatualWeek() {
        // Time elapsed 14 days, 337 - 1 hours
        Mockito.when(meterReadingService.getPrevNatualWeekReadings(any(String.class)))
                .thenReturn(Optional.of(generator.generate(169, 1, 3600, Instant.now().minusSeconds(3600 * 24 * 7))));

        Map<String, BigDecimal> costMap = pricePlanService.getPrevWeekConsumptionCostOfElectricityReadings(METER_ID_0).get();

        assertEquals(BigDecimal.valueOf(168).stripTrailingZeros(), costMap.get(PLAN_ID_0).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(1680).stripTrailingZeros(), costMap.get(PLAN_ID_1).stripTrailingZeros());
    }
}
