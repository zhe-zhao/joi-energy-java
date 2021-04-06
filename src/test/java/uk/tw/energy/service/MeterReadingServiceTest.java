package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.generator.ElectricityReadingsGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MeterReadingServiceTest {

    private final ElectricityReadingsGenerator generator = new ElectricityReadingsGenerator();
    private MeterReadingService meterReadingService;

    @BeforeEach
    public void setUp() {
        Map<String, List<ElectricityReading>> associatedReadings =
                List.of("meter_id_0", "meter_id_1").stream().collect(Collectors.toMap(String::valueOf, id -> generator.generate(337, 1, 3600)));
        meterReadingService = new MeterReadingService(associatedReadings);
    }

    @Test
    public void givenMeterIdThatDoesNotExistShouldReturnNull() {
        assertThat(meterReadingService.getReadings("unknown-id")).isEqualTo(Optional.empty());
    }

    @Test
    public void givenMeterReadingThatExistsShouldReturnMeterReadings() {
        meterReadingService.storeReadings("random-id", new ArrayList<>());
        assertThat(meterReadingService.getReadings("random-id")).isEqualTo(Optional.of(new ArrayList<>()));
        assertEquals(337, meterReadingService.getReadings("meter_id_0").get().size());
    }

    @Test
    public void givenMeterReadingThatExistsShouldReturnPrevNatualWeekReadings() {
        meterReadingService.storeReadings("random-id", new ArrayList<>());
        assertEquals(0, meterReadingService.getPrevNatualWeekReadings("random-id").get().size());
        assertEquals(168, meterReadingService.getPrevNatualWeekReadings("meter_id_0").get().size());
    }
}
