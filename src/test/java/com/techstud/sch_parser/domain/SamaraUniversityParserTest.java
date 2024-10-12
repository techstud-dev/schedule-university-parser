package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.TestDefaultUtils;
import com.techstud.sch_parser.domain.impl.SamaraUniversityParser;
import com.techstud.sch_parser.model.Schedule;
import org.junit.jupiter.api.Test;
import lombok.extern.log4j.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@Log4j2
public class SamaraUniversityParserTest {

    private final Parser underTest = new SamaraUniversityParser();;

    @Test
    public void checkReturnCorrectTimetableMastersEighteenHoursEven() throws Exception {
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(
                        TestDefaultUtils.eighteenHoursStartGroup
                )
        );
        log.info("Data from test ---");
        log.info(result);

        assertThat(result)
                .isNotEmpty();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenHours() throws Exception {
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(
                        TestDefaultUtils.nightHoursStartGroup
                )
        );
        log.info(result);


        assertThat(result)
                .isNotNull();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenAndEleventhHoursEven() throws Exception {

        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(
                        TestDefaultUtils.eleventhAndEightHoursEndGroup
                )
        );
        log.info(result);


        assertThat(result)
                .isNotNull();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenAndEleventhHoursNotEven() throws Exception {
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(
                        TestDefaultUtils.eleventhAndEightHoursEndGroup
                )
        );
        log.info(result);

        assertThat(result)
                .isNotNull();
    }

}
