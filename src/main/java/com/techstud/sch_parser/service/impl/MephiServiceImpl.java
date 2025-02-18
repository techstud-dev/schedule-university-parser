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
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.parseDayOfWeek;
import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.mapMephiLessonTypeToScheduleType;

@Slf4j
@Service
public class MephiServiceImpl implements MappingServiceRef<List<Document>> {

    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        log.info("Start mapping MEPHI data to schedule");

        if (source.size() < 2) {
            throw new IllegalArgumentException("Not enought documents for even and odd weeks");
        }

        var evenWeekDocument = source.get(0);
        var oddWeekDocument = source.get(1);

        var evenWeekSchedule = getMephiWeekSchedule(evenWeekDocument);
        var oddWeekSchedule = getMephiWeekSchedule(oddWeekDocument);

        var schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        schedule.setSnapshotDate(new Date());

        log.info("MEPHI mapping {} completed", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> getMephiWeekSchedule(Document document) {
        var weekSchedule = new LinkedHashMap<DayOfWeek, ScheduleDay>();
        var elements = document.select(".lesson-wday, .list-group");

        var currentDayOfWeek = new Object() { DayOfWeek value = null; };

        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        elements.forEach(element -> {
            if (element.hasClass("lesson-wday")) {
                currentDayOfWeek.value = parseDayOfWeek(element.text());
            } else if (element.hasClass("list-group") && currentDayOfWeek.value != null) {
                LocalDate date = startOfWeek.with(currentDayOfWeek.value);
                weekSchedule.put(currentDayOfWeek.value, getMephiScheduleDay(element, date));
            }
        });
        return weekSchedule;
    }

    private ScheduleDay getMephiScheduleDay(Element element, LocalDate date) {
        var scheduleDayMap = element.select("div.list-group-item.d-xs-flex").stream()
                .collect(Collectors.toMap(
                        groupElement -> parseMephiTimeSheet(Objects.requireNonNull(groupElement.selectFirst(".lesson-time")).text()),
                        this::getMephiScheduleObjects,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setLocalDate(date);
        scheduleDay.setLessons(scheduleDayMap);
        return scheduleDay;
    }

    private List<ScheduleObject> getMephiScheduleObjects(Element element) {
        return element.select(".lesson-lessons > .lesson").stream().map(lessonElement -> {
            var scheduleObject = new ScheduleObject();

            var fullText = lessonElement.ownText().trim();
            var ownTextParts = fullText.split("Subgroup");
            var lessonName = ownTextParts[0].trim().replaceAll("[,\\s]+$", "");

            List<String> groups = new ArrayList<>();
            if (ownTextParts.length > 1) {
                groups.add("Subgroup " + ownTextParts[1].trim());
            }

            var placeElement = lessonElement.selectFirst("i.fa-map-marker + a.text-nowrap");
            var place = placeElement != null ? placeElement.text().trim() :
                    Optional.ofNullable(lessonElement.selectFirst("span.label.label-purple"))
                            .map(Element::text).map(String::trim).orElse(null);

            var teachers = lessonElement.select("span.text-nowrap a").stream()
                    .map(Element::text).map(String::trim).toList();

            var type = lessonElement.select(".label-lesson").text().trim();
            if (type.isEmpty()) {
                var titleElement = lessonElement.selectFirst(".title");
                type = titleElement != null && titleElement.text().contains("Reserve") ? "Reserve" : "UNKNOWN";
            }

            scheduleObject.setType(mapMephiLessonTypeToScheduleType(type));
            scheduleObject.setName(lessonName);
            scheduleObject.setTeacher(teachers.isEmpty() ? null : String.join(", ", teachers));
            scheduleObject.setPlace(place);
            scheduleObject.setGroups(groups);

            return scheduleObject;
        }).toList();
    }

    private TimeSheet parseMephiTimeSheet(String timeRange) {
        var times = timeRange.split("—");
        return (times.length == 2) ? new TimeSheet(times[0].trim(), times[1].trim()) : null;
    }


}
