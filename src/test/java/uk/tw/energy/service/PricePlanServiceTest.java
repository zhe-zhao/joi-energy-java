package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.generator.ElectricityReadingsGenerator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricePlanServiceTest {
    public static final String METER_ID_0 = "meter_0";
    public static final String METER_ID_1 = "meter_1";
    public static final String PLAN_ID_0 = "plan_id_0";
    public static final String PLAN_ID_1 = "plan_id_1";
    private PricePlanService pricePlanService;

    @BeforeEach
    public void setup() {
        final List<PricePlan> pricePlans = List.of(
                new PricePlan(PLAN_ID_0, "Supplier 0", BigDecimal.ONE, emptyList()),
                new PricePlan(PLAN_ID_1, "Supplier 1", BigDecimal.TEN, emptyList())
        );

        ElectricityReadingsGenerator generator = new ElectricityReadingsGenerator();
        Map<String, List<ElectricityReading>> meterAssociatedReadings = new HashMap<>();
        // Time elapsed 360 seconds
        List.of(METER_ID_0, METER_ID_1).stream().forEach(meterId -> meterAssociatedReadings.put(meterId, generator.generate(37, 1)));
        final MeterReadingService meterReadingService = new MeterReadingService(meterAssociatedReadings);

        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
    }

    @Test
    public void givenMeterId_shouldGetConsumptionCost() {
        Map<String, BigDecimal> costMap = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(METER_ID_0).get();

        assertEquals(BigDecimal.valueOf(0.1).stripTrailingZeros(), costMap.get(PLAN_ID_0).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(1).stripTrailingZeros(), costMap.get(PLAN_ID_1).stripTrailingZeros());
        assertTrue(costMap.get(PLAN_ID_0).compareTo(BigDecimal.valueOf(0.1)) == 0);
        assertTrue(costMap.get(PLAN_ID_1).compareTo(BigDecimal.valueOf(1)) == 0);
    }

}
