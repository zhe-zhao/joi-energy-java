package uk.tw.energy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricePlanComparatorControllerTest {

    private static final String PRICE_PLAN_1_ID = "test-supplier";
    private static final String PRICE_PLAN_2_ID = "best-supplier";
    private static final String PRICE_PLAN_3_ID = "second-best-supplier";
    private static final String SMART_METER_ID = "smart-meter-id";
    private PricePlanComparatorController controller;
    private MeterReadingService meterReadingService;
    private AccountService accountService;

    @BeforeEach
    public void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>());
        PricePlan pricePlan1 = new PricePlan(PRICE_PLAN_1_ID, null, BigDecimal.TEN, null);
        PricePlan pricePlan2 = new PricePlan(PRICE_PLAN_2_ID, null, BigDecimal.ONE, null);
        PricePlan pricePlan3 = new PricePlan(PRICE_PLAN_3_ID, null, BigDecimal.valueOf(2), null);

        List<PricePlan> pricePlans = Arrays.asList(pricePlan1, pricePlan2, pricePlan3);
        PricePlanService tariffService = new PricePlanService(pricePlans, meterReadingService);

        Map<String, String> meterToTariffs = new HashMap<>();
        meterToTariffs.put(SMART_METER_ID, PRICE_PLAN_1_ID);
        accountService = new AccountService(meterToTariffs);

        controller = new PricePlanComparatorController(tariffService, accountService);
    }

    @Test
    public void shouldCalculateCostForMeterReadingsForEveryPricePlan() {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(15.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        Map<String, BigDecimal> expectedPricePlanToCost = new HashMap<>();
        expectedPricePlanToCost.put(PRICE_PLAN_1_ID, BigDecimal.valueOf(100.0).setScale(3));
        expectedPricePlanToCost.put(PRICE_PLAN_2_ID, BigDecimal.valueOf(10.0).setScale(3));
        expectedPricePlanToCost.put(PRICE_PLAN_3_ID, BigDecimal.valueOf(20.0).setScale(3));

        Map<String, Object> expected = new HashMap<>();
        expected.put(PricePlanComparatorController.PRICE_PLAN_ID_KEY, PRICE_PLAN_1_ID);
        expected.put(PricePlanComparatorController.PRICE_PLAN_COMPARISONS_KEY, expectedPricePlanToCost);
        assertThat(controller.calculatedCostForEachPricePlan(SMART_METER_ID).getBody()).isEqualTo(expected);
    }

    @Test
    public void shouldCalculateCostForMeterReadingsInPrevNatualWeek() {
        Instant now = Instant.now();
        ElectricityReading readingCurrentWeek1 = new ElectricityReading(now.minusSeconds(3600), BigDecimal.valueOf(15.0));
        ElectricityReading readingCurrentWeek2 = new ElectricityReading(now, BigDecimal.valueOf(5.0));
        int secondsInOneWeek = 3600 * 24 * 7;
        ElectricityReading readingPrevWeek1 = new ElectricityReading(now.minusSeconds(secondsInOneWeek), BigDecimal.valueOf(3.0));
        ElectricityReading readingPrevWeek2 = new ElectricityReading(now.minusSeconds(secondsInOneWeek + 3600), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(readingCurrentWeek1, readingCurrentWeek2, readingPrevWeek1, readingPrevWeek2));

        Map<String, BigDecimal> expectedPricePlanToCost = new HashMap<>();
        expectedPricePlanToCost.put(PRICE_PLAN_1_ID, BigDecimal.valueOf(40.0).setScale(3));
        expectedPricePlanToCost.put(PRICE_PLAN_2_ID, BigDecimal.valueOf(4.0).setScale(3));
        expectedPricePlanToCost.put(PRICE_PLAN_3_ID, BigDecimal.valueOf(8.0).setScale(3));

        Map<String, Object> expected = new HashMap<>();
        expected.put(PricePlanComparatorController.PRICE_PLAN_ID_KEY, PRICE_PLAN_1_ID);
        expected.put(PricePlanComparatorController.PRICE_PLAN_COMPARISONS_KEY, expectedPricePlanToCost);
        assertThat(controller.calculatedPrevNatualWeekCostForEachPricePlan(SMART_METER_ID).getBody()).isEqualTo(expected);
        assertEquals(expected, controller.calculatedPrevNatualWeekCostForEachPricePlan((SMART_METER_ID)).getBody());
    }

    @Test
    public void shouldRecommendCheapestPricePlansNoLimitForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(35.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(9.5).setScale(3)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(19.0).setScale(3)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_1_ID, BigDecimal.valueOf(95.0).setScale(3)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, null).getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void shouldRecommendLimitedCheapestPricePlansForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(2700), BigDecimal.valueOf(5.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(20.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(9.375).setScale(3)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(18.750).setScale(3)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, 2).getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void shouldRecommendCheapestPricePlansMoreThanLimitAvailableForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(14.00).setScale(3)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(28.00).setScale(3)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_1_ID, BigDecimal.valueOf(140.00).setScale(3)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, 5).getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void givenNoMatchingMeterIdShouldReturnNotFound() {
        assertThat(controller.calculatedCostForEachPricePlan("not-found").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
