package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.ScheduleObject;
import com.techstud.sch_parser.model.TimeSheet;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.miitParseDayOfWeeK;
import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.mapMiitLessonTypeToScheduleType;

@Slf4j
@Service
public class MiitServiceImpl implements MappingServiceRef<List<Document>> {

    @Override
    public Schedule map(List<Document> source) {
        log.info("Start mapping MIIT data to schedule");

        Schedule schedule = new Schedule();
        if (source == null || source.size() < 2) {
            log.error("Insufficient documents to parse even and odd week MIIT schedules ");
            return schedule;
        }

        schedule.setEvenWeekSchedule(parseMiitWeekSchedule(source.get(0)));
        schedule.setOddWeekSchedule(parseMiitWeekSchedule(source.get(1)));

        log.info("Mapping MIIT data to schedule {} finished.", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> parseMiitWeekSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new EnumMap<>(DayOfWeek.class);
        Arrays.stream(DayOfWeek.values()).forEach(day -> weekSchedule.put(day, new ScheduleDay()));

        Elements rows = document.select("tr");
        if (rows.isEmpty()) {
            log.warn("No rows found in the MIIT document");
            return weekSchedule;
        }

        var dayOfWeekMapping = extractMiitDayOfWeekMapping(rows.get(0));
        var allTimeSheets = extractAllMiitTimeSheets(rows);

        weekSchedule.forEach((day, scheduleDay) ->
                allTimeSheets.forEach(timeSheet ->
                        scheduleDay.getLessons().putIfAbsent(timeSheet, new ArrayList<>())
                )
        );

        rows.stream().skip(1).forEach(row ->
                processMiitLessonRow(row, weekSchedule, dayOfWeekMapping)
        );

        return weekSchedule;
    }


    private Map<Integer, DayOfWeek> extractMiitDayOfWeekMapping(Element headerRow) {
        Map<Integer, DayOfWeek> dayOfWeekMapping = new LinkedHashMap<>();
        Elements dayHeaders = headerRow.select("th");

        for (int i = 1; i < dayHeaders.size(); i++) {
            String dayOfWeekText = dayHeaders.get(i).text().split(" ")[0].trim();
            int finalI = i;
            Optional.ofNullable(miitParseDayOfWeeK(dayOfWeekText))
                    .ifPresentOrElse(
                            day -> dayOfWeekMapping.put(finalI - 1, day),
                            () -> log.warn("Could not parse MIIT day of week for text: {}", dayOfWeekText)
                    );
        }
        return dayOfWeekMapping;
    }

    private void processMiitLessonRow(Element lessonRow, Map<DayOfWeek, ScheduleDay> weekSchedule,
                                      Map<Integer, DayOfWeek> dayOfWeekMapping) {
        Elements cells = lessonRow.select("td");
        TimeSheet timeSheet = getMiitTimeSheet(cells.first());

        if (timeSheet == null) {
            log.warn("Skipping MIIT row due to invalid time format");
            return;
        }

        for (int i = 1; i < cells.size(); i++) {
            Element lessonCell = cells.get(i);
            DayOfWeek dayOfWeek = dayOfWeekMapping.get(i - 1);

            if (dayOfWeek == null || lessonCell.text().isBlank()) {
                continue;
            }

            List<ScheduleObject> scheduleObjects = getMiitScheduleObjects(lessonCell);

            scheduleObjects.forEach(scheduleObject ->
                    weekSchedule.get(dayOfWeek)
                            .getLessons()
                            .get(timeSheet)
                            .add(scheduleObject)
            );
        }
    }

    private List<ScheduleObject> getMiitScheduleObjects(Element lessonElement) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();

        Elements lessonParts = lessonElement.select("div.timetable__grid-day-lesson");
        for (Element lessonPart : lessonParts) {
            ScheduleObject scheduleObject = new ScheduleObject();

            Optional.ofNullable(lessonPart.selectFirst("span.timetable__grid-text_gray"))
                    .ifPresent(el -> scheduleObject.setType(mapMiitLessonTypeToScheduleType(el.text().trim())));

            String lessonName = lessonPart.ownText().trim();
            scheduleObject.setName(lessonName);

            assert lessonPart.parent() != null;
            Optional.ofNullable(lessonPart.parent().selectFirst("a.icon-academic-cap"))
                    .ifPresent(el -> scheduleObject.setTeacher(el.attr("title").trim()));

            String place = String.join(", ", lessonPart.parent().select("a.icon-location").eachText());
            scheduleObject.setPlace(place);

            List<String> groups = lessonPart.parent()
                    .select("span.icon-community, a.icon-community")
                    .eachText();
            scheduleObject.setGroups(groups);

            scheduleObjects.add(scheduleObject);
        }

        return scheduleObjects;
    }

    private TimeSheet getMiitTimeSheet(Element timeCell) {
        if (timeCell == null) {
            log.error("MIIT time cell not found.");
            return null;
        }

        String timeText = Optional.ofNullable(timeCell.selectFirst(".timetable__grid-text_gray"))
                .map(Element::text)
                .orElse(null);
        if (timeText == null) {
            log.error("Time element with gray text not found in MIIT time cell.");
            return null;
        }

        String[] timeParts = timeText.split(" — ");
        return (timeParts.length == 2)
                ? new TimeSheet(timeParts[0].trim(), timeParts[1].trim())
                : null;
    }


    private List<TimeSheet> extractAllMiitTimeSheets(Elements rows) {
        return rows.stream()
                .skip(1)
                .map(row -> getMiitTimeSheet(row.select("td").first()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TimeSheet::getFrom))
                .distinct()
                .toList();
    }


}
