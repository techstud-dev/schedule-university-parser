package com.funtikov.sch_parser.domain;

import com.funtikov.sch_parser.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

import static com.funtikov.sch_parser.model.ScheduleType.returnTypeByRuName;

@Component
@Slf4j
public class SamaraUniversityParser implements Parser {

    @Override
    public Schedule parseSchedule(Long groupId) throws IOException {

        final String[] evenParameters = {String.valueOf(groupId), "2"};
        final String[] oddParameters = {String.valueOf(groupId), "1"};
        String samaraUniversityScheduleUrl = "https://ssau.ru/rasp?groupId={0}&selectedWeek={1}";
        final String evenUrl = MessageFormat.format(samaraUniversityScheduleUrl, evenParameters[0], evenParameters[1]);
        final String oddUrl = MessageFormat.format(samaraUniversityScheduleUrl, oddParameters[0], oddParameters[1]);

        try {
            Document evenDoc = Jsoup
                    .connect(evenUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get();

            String responseBody = evenDoc
                    .body()
                    .html();  // Получаем HTML тела ответа
            log.info("Response body: {}",
                    responseBody
            );

            Document oddDoc = Jsoup
                    .connect(oddUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get();
            Map<Integer, Map<DayOfWeek, ScheduleDay>> weekSchedulesMap = getSchedules(evenDoc, oddDoc);
            Schedule schedule = new Schedule();
            schedule.setEvenWeekSchedule(weekSchedulesMap.get(2));
            schedule.setOddWeekSchedule(weekSchedulesMap.get(1));
            schedule.setSnapshotDate(new Date());
            return schedule;
        } catch (HttpStatusException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private Map<Integer, Map<DayOfWeek, ScheduleDay>> getSchedules(Document evenDoc, Document oddDoc) {
        Map<Integer, Map<DayOfWeek, ScheduleDay>> scheduleDayMap = new LinkedHashMap<>();
        Element evenElement = evenDoc.getElementsByClass("schedule").first();
        Element oddElement = oddDoc.getElementsByClass("schedule").first();
        scheduleDayMap.put(1, new HashMap<>());
        scheduleDayMap.put(2, new HashMap<>());

        //evenElement work
        assert evenElement != null;
        List<Element> scheduleItemElements = evenElement.
                getElementsByClass("schedule__item")
                .stream()
                .toList();
        scheduleItemElements = scheduleItemElements.subList(7, scheduleItemElements.size() - 1);

        List<Element> scheduleTimeSheets = evenElement
                .getElementsByClass("schedule__time")
                .stream()
                .toList();

        int chunkCount = (int) Math.ceil((double) scheduleItemElements.size() / (double) scheduleTimeSheets.size());

        Map<Integer, List<Element>> scheduleItemElementsChunk = new LinkedHashMap<>();
        int from = 0;
        int to = 5;
        for (int i = 0; i < chunkCount; i++) {
            scheduleItemElementsChunk.put(i, scheduleItemElements.subList(from, to));
            from += chunkCount;
            to += chunkCount;
        }

        for (int scheduleTimeSheet = 0; scheduleTimeSheet < scheduleTimeSheets.size(); scheduleTimeSheet++) {
            if (scheduleTimeSheet == 0) {
                scheduleDayMap.put(2,
                        getSchedulesForWeek(scheduleTimeSheets.get(scheduleTimeSheet),
                                scheduleItemElementsChunk.get(scheduleTimeSheet)));
            }
        }
        return scheduleDayMap;
    }

    private Map<DayOfWeek, ScheduleDay> getSchedulesForWeek(Element timeSheetElement, List<Element> scheduleItemElements) {
        TimeSheet timeSheet = new TimeSheet();
        List<String> timeSheetAttributes =
                timeSheetElement.getElementsByClass("schedule__time-item").stream().map(Element::text).toList();
        timeSheet.setFrom(LocalTime.parse(timeSheetAttributes.get(0)));
        timeSheet.setTo(LocalTime.parse(timeSheetAttributes.get(1)));
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();
        Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();
        lessons.put(timeSheet, List.of(new ScheduleObject()));
        scheduleDayMap.put(DayOfWeek.MONDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.TUESDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.WEDNESDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.THURSDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.FRIDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.SATURDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.SUNDAY, new ScheduleDay());

        for (int i = 0; i < scheduleItemElements.size(); i++) {
            scheduleDayMap.get(DayOfWeek.of(i + 1)).setLessons(lessons);
            scheduleDayMap.get(DayOfWeek.of(i + 1)).getLessons().put(timeSheet, getScheduleInfo(scheduleItemElements.get(i)));
        }
        return scheduleDayMap;
    }

    private List<ScheduleObject> getScheduleInfo(Element scheduleElement) {
        //FIXME: Парсится адекватно только временной промежуток. Исправить
        String lessonType = scheduleElement
                .getElementsByClass("schedule__lesson-type-chip lesson-type-3__bg")
                .text();
        String lessonName = scheduleElement.getElementsByClass("body-text schedule__discipline").text();
        String lessonPlace = scheduleElement.getElementsByClass("caption-text schedule__place").text();
        String lessonTeacher = scheduleElement.getElementsByClass("schedule__teacher").text();
        String lessonGroups = scheduleElement.getElementsByClass("schedule__groups").text();
        ScheduleObject scheduleObject = new ScheduleObject();

        scheduleObject.setName(lessonName);
        scheduleObject.setPlace(lessonPlace);
        scheduleObject.setTeacher(lessonTeacher);

        try {
            scheduleObject.setType(returnTypeByRuName(lessonType.trim()));
        } catch (IllegalArgumentException e) {
            log.error("Illegal lesson type = {}", lessonType, e);
        }

        return List.of(scheduleObject);
    }

}
