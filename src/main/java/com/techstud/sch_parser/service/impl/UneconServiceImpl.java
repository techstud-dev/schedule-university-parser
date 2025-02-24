package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.uneconParseDayOfWeek;

@Slf4j
@Service
public class UneconServiceImpl implements MappingServiceRef<List<Document>> {

    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        log.info("Start mapping UNECON data to schedule");
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();

        boolean isEvenWeekEmpty = isDocumentEmpty(source.get(0));
        boolean isOddWeekEmpty = isDocumentEmpty(source.get(1));

        log.info("Is even week empty: {}, Is odd week empty: {}", isEvenWeekEmpty, isOddWeekEmpty);

        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = 0; i < source.size(); i++) {
            Document document = source.get(i);
            if (document == null) {
                log.warn("Document is empty");
                continue;
            }

            boolean isCurrentWeekEmpty = (i == 0) ? isEvenWeekEmpty : isOddWeekEmpty;
            if (isCurrentWeekEmpty) {
                log.warn("Skipping week {} because it's empty", (i == 0) ? "even" : "odd");
                continue;
            }

            Elements elements = document.select("table tbody tr");
            log.info("Found elements: {}", elements.size());

            DayOfWeek currentDayOfWeek;
            ScheduleDay currentWeekDay = null;
            LocalDate weekStartDate = (i == 0) ? startOfWeek : startOfWeek.plusWeeks(1);

            for (Element row : elements) {
                if (row.hasClass("new_day_border")) {
                    Element nextRow = row.nextElementSibling();
                    if (nextRow != null && nextRow.hasClass("new_day")) {
                        String dayText = nextRow.select(".day").text().trim();

                        try {
                            currentDayOfWeek = uneconParseDayOfWeek(dayText);
                            LocalDate currentDate = weekStartDate.plusDays(currentDayOfWeek.getValue() - 1);
                            currentWeekDay = new ScheduleDay(currentDate);
                            currentWeekDay.setLocalDate(currentDate);
                            if (i == 0) {
                                evenWeekSchedule.put(currentDayOfWeek, currentWeekDay);
                            } else {
                                oddWeekSchedule.put(currentDayOfWeek, currentWeekDay);
                            }
                        } catch (IllegalArgumentException e) {
                            log.error("Error while parsing day of week: {}", dayText, e);
                        }
                    }
                } else if (currentWeekDay != null) {
                    getUneconScheduleDay(row, currentWeekDay);
                }
            }
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping UNECON data to schedule {} finished", schedule);
        return schedule;
    }

    private void getUneconScheduleDay(Element element, ScheduleDay scheduleDay) {
        TimeSheet timeSheet = parseUneconTimeSheet(element);
        if (timeSheet == null) {
            log.warn("Cant process timesheet in the element: {}", element);
            return;
        }

        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();
        lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>()).addAll(parseUneconScheduleObject(element));
    }

    private List<ScheduleObject> parseUneconScheduleObject(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();

        String name = element.select(".predmet .predmet").text();
        List<String> types = new ArrayList<>();

        Matcher typeMatcher = Pattern.compile("\\s*\\((.*?)\\)").matcher(name);

        while (typeMatcher.find()) {
            types.add(typeMatcher.group(1));
        }

        for (String type : types) {
            name = name.replace(" (" + type + ")", "").replace("(" + type + ")", "").trim();
        }

        String teacher = element.select(".predmet .prepod a").text();
        String place = element.select(".predmet .aud").text();
        place = place.replace("ПОКАЗАТЬ НА СХЕМЕ ", "").trim();

        ScheduleObject scheduleObject = new ScheduleObject();

        if (teacher.isEmpty()) {
            scheduleObject.setTeacher(null);
        } else {
            scheduleObject.setTeacher(teacher);
        }

        scheduleObject.setName(name);
        scheduleObject.setPlace(place);

        for (String type : types) {
            ScheduleType lessonType = ScheduleType.returnTypeByRuName(type.trim());
            if (lessonType != ScheduleType.UNKNOWN) {
                scheduleObject.setType(lessonType);
                break;
            }
        }
        if (scheduleObject.getType() == null) {
            scheduleObject.setType(ScheduleType.UNKNOWN);
        }

        scheduleObjects.add(scheduleObject);
        return scheduleObjects;
    }

    private TimeSheet parseUneconTimeSheet(Element element) {
        Element timeCell = element.select(".no_480.time .time").first();
        if (timeCell == null) {
            log.warn("Didnt found element in: {}", element);
            return null;
        }

        String timeText = timeCell.text().trim();

        try {
            String[] times = timeText.split(" - ");
            LocalTime fromTime = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime toTime = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));

            return new TimeSheet(fromTime, toTime);
        } catch (DateTimeParseException e) {
            log.error("Error while parsing timesheet: {}", timeText, e);
            return null;
        }
    }

    private boolean isDocumentEmpty(Document document) {
        if (document == null) {
            return true;
        }
        Elements rows = document.select("table tbody tr");
        return rows.isEmpty();
    }

}
