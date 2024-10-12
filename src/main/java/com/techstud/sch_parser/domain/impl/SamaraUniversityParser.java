package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.techstud.sch_parser.model.ScheduleDayOfWeekParse.*;
import static com.techstud.sch_parser.model.ScheduleType.returnTypeByRuName;

@Component
@Slf4j
public class SamaraUniversityParser implements Parser {

    @Override
    public Schedule parseSchedule(Long groupId) throws IOException {

        final String[] evenParameters = {String.valueOf(groupId), "2"};
        log.info(Arrays.toString(evenParameters));
        final String[] oddParameters = {String.valueOf(groupId), "1"};
        log.info(Arrays.toString(oddParameters));

        String samaraUniversityScheduleUrl = "https://ssau.ru/rasp?groupId={0}&selectedWeek={1}";

        final String evenUrl = MessageFormat.format(
                samaraUniversityScheduleUrl,
                evenParameters[0],
                evenParameters[1]
        );
        log.info(evenUrl);
        final String oddUrl = MessageFormat.format(
                samaraUniversityScheduleUrl,
                oddParameters[0],
                oddParameters[1]
        );
        log.info(oddUrl);

        try {
            Document evenDoc = Jsoup
                    .connect(evenUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get();

            Document oddDoc = Jsoup
                    .connect(oddUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get();

            Map<Integer, Map<DayOfWeek, ScheduleDay>> weekSchedulesMap = getSchedules(
                    evenDoc,
                    oddDoc
            );

            Schedule schedule = new Schedule();
            schedule.setEvenWeekSchedule(
                    weekSchedulesMap.get(2)
            );

            schedule.setOddWeekSchedule(
                    weekSchedulesMap.get(1));
            schedule.setSnapshotDate(new Date()
            );

            return schedule;

        } catch (HttpStatusException e) {

            log.error(
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    private Map<Integer, Map<DayOfWeek, ScheduleDay>> getSchedules(Document evenDoc, Document oddDoc) {
        Map<Integer, Map<DayOfWeek, ScheduleDay>> scheduleDayMap = new LinkedHashMap<>();

        Element evenElement = evenDoc
                .getElementsByClass("schedule")
                .first();
        Element oddElement = oddDoc
                .getElementsByClass("schedule")
                .first();

        scheduleDayMap.put(1,
                parseWeekSchedule(oddDoc)
        );
        scheduleDayMap.put(2,
                parseWeekSchedule(evenDoc)
        );

        if (evenElement == null || oddElement == null) {
            log.error("Failed to find an element with class ‘schedule’ on the even schedule page.");
            return scheduleDayMap;
        }

        List<Element> scheduleItemElements = evenElement
                .getElementsByClass("schedule__item")
                .stream()
                .toList();

        List<Element> scheduleItemElementsOdd = oddElement
                .getElementsByClass("schedule__item")
                .stream()
                .toList();

        if (scheduleItemElements.size() < 8 && scheduleItemElementsOdd.size() < 8) {
            log.warn("Not enough schedule items: {}",
                    scheduleItemElements.size() + "_" + scheduleItemElementsOdd.size()
            );
            return scheduleDayMap;
        }

        scheduleItemElements = scheduleItemElements.subList(7, scheduleItemElements.size());

        scheduleItemElementsOdd = scheduleItemElementsOdd.subList(7, scheduleItemElementsOdd.size());

        List<Element> scheduleTimeSheets = evenElement
                .getElementsByClass("schedule__time")
                .stream()
                .toList();

        List<Element> scheduleTimeSheetsOdd = oddElement
                .getElementsByClass("schedule__time")
                .stream()
                .toList();

        if (scheduleTimeSheets.isEmpty() && scheduleTimeSheetsOdd.isEmpty()) {
            log.warn("There are no temporary schedule items to parsing.");
            return scheduleDayMap;
        };

        // FIXME: Мне кажется, что все проблемы находятся тут (?)

        Map<Integer, List<Element>> mapItemEven = scheduleItemElementsChunk(scheduleTimeSheets, scheduleItemElements);
        Map<Integer, List<Element>> mapItemOdd = scheduleItemElementsChunk(scheduleTimeSheetsOdd, scheduleItemElementsOdd);

        returnScheduleMap(mapItemEven, scheduleTimeSheets, scheduleDayMap, 2);
        returnScheduleMap(mapItemOdd, scheduleTimeSheetsOdd, scheduleDayMap, 1);

        log.info(scheduleDayMap.toString());
        return scheduleDayMap;
    }

    private void mergeScheduleDayMaps(Map<DayOfWeek, ScheduleDay> existingMap, Map<DayOfWeek, ScheduleDay> newMap) {
        for (Map.Entry<DayOfWeek, ScheduleDay> entry : newMap.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            ScheduleDay newScheduleDay = entry.getValue();

            if (existingMap.containsKey(dayOfWeek)) {
                ScheduleDay existingScheduleDay = existingMap.get(dayOfWeek);
                // Объединяем уроки
                Map<TimeSheet, List<ScheduleObject>> existingLessons = existingScheduleDay.getLessons();
                Map<TimeSheet, List<ScheduleObject>> newLessons = newScheduleDay.getLessons();

                existingLessons.putAll(newLessons);
            } else {
                existingMap.put(dayOfWeek, newScheduleDay);
            }
        }
    }

    private Map<DayOfWeek, ScheduleDay> getSchedulesForWeek(Element timeSheetElement, List<Element> scheduleItemElements) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new TreeMap<>(getDayOfWeekComparator());

        if (scheduleItemElements.isEmpty()) {
            log.warn("No elements for parsing");
            return scheduleDayMap;
        }

        return IntStream.range(0, scheduleItemElements.size())
                .boxed()
                .collect(Collectors.toMap(
                        index -> DayOfWeek.of((index % 7) + 1),
                        index -> {
                            ScheduleDay scheduleDay = new ScheduleDay();
                            Map<TimeSheet, List<ScheduleObject>> dayLessons = new LinkedHashMap<>();
                            dayLessons.put(
                                    parseTimeSheet(timeSheetElement),
                                    getScheduleInfo(scheduleItemElements.get(
                                            index)
                                    )
                            );
                            scheduleDay.setLessons(dayLessons);
                            return scheduleDay;
                        },
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }

    public TimeSheet parseTimeSheet(Element timeSheetElement) {
        List<String> timeSheetAttributes = timeSheetElement
                        .getElementsByClass("schedule__time-item")
                        .stream()
                        .map(Element::text)
                        .toList();

        if (timeSheetAttributes.size() < 2) {
            log.warn(
                    "Not enough time elements for parsing: {}",
                    timeSheetAttributes);
            throw new IllegalArgumentException("Not enough time elements for parsing.");
        }

        TimeSheet timeSheet = new TimeSheet();

        timeSheet.setFrom(LocalTime.parse(timeSheetAttributes.get(0)));

        timeSheet.setTo(LocalTime.parse(timeSheetAttributes.get(1)));

        return timeSheet;
    }

    private List<ScheduleObject> getScheduleInfo(Element scheduleElement) {

        // Переделал getElementByClass в селекторы ( .select() ) для большей читаемости, производительности и гибкости
        String lessonType = scheduleElement
                .select(".schedule__lesson-type-chip")
                .text();

        String lessonName = scheduleElement
                .select(".schedule__discipline")
                .text();

        String lessonPlace = scheduleElement
                .select(".schedule__place")
                .text();

        String lessonTeacher = scheduleElement
                .select(".schedule__teacher")
                .text();

        List<String> lessonGroups = Collections.singletonList(scheduleElement
                .select(".schedule__groups")
                .text());

        ScheduleObject scheduleObject = new ScheduleObject();

        scheduleObject.setName(lessonName.isEmpty() ? null : lessonName);

        scheduleObject.setPlace(lessonPlace.isEmpty() ? null : lessonPlace);

        scheduleObject.setTeacher(lessonTeacher.isEmpty() ? null : lessonTeacher);

        scheduleObject.setGroups(lessonGroups);

        try {
            scheduleObject.setType(
                    returnTypeByRuName(
                            lessonType.trim()
                    )
            );
        } catch (IllegalArgumentException error) {
            log.error(
                    "Illegal lesson type = {}",
                    lessonType,
                    error
            );
        }

        return List.of(scheduleObject);
    }

    private Map<DayOfWeek, ScheduleDay> parseWeekSchedule(Document doc) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new TreeMap<>(Comparator.comparingInt(DayOfWeek::getValue));

        Element scheduleElement = doc.getElementsByClass("schedule").first();
        if (scheduleElement == null) {
            log.error("Failed to find an element with class ‘schedule’ on the schedule page.");
            return scheduleDayMap;
        }

        // Получаем все элементы, соответствующие дням недели
        Elements dayElements = scheduleElement.select(".schedule__day");
        for (Element dayElement : dayElements) {
            // Извлекаем название дня недели
            String dayName = dayElement.select(".schedule__day-title").text().trim();
            DayOfWeek dayOfWeek = parseDayOfWeek(dayName);
            //log.info("Processing the day of the week: {}", dayOfWeek);

            ScheduleDay scheduleDay = new ScheduleDay();
            Map<TimeSheet, List<ScheduleObject>> lessonsMap = new LinkedHashMap<>();

            // Получаем все занятия для данного дня
            Elements lessonElements = dayElement.select(".schedule__lesson");
            for (Element lessonElement : lessonElements) {
                TimeSheet timeSheet = parseTimeSheet(Objects.requireNonNull(lessonElement.selectFirst(".schedule__time")));
                List<ScheduleObject> lessons = getScheduleInfo(lessonElement);
                lessonsMap.put(timeSheet, lessons);
            }

            scheduleDay.setLessons(lessonsMap);
            scheduleDayMap.put(dayOfWeek, scheduleDay);
        }

        return scheduleDayMap;
    }

    private Map<Integer, List<Element>> scheduleItemElementsChunk(List<Element> scheduleTimeSheets, List<Element> scheduleItemElements ){
        Map<Integer, List<Element>> result = new LinkedHashMap<>();
        var from = 0;
        for (var i = 0; i < (Math.ceil((double) scheduleItemElements.size() / (double) scheduleTimeSheets.size())); i++) {
            var to = Math.min(
                    from + (scheduleItemElements.size() / scheduleTimeSheets.size()),
                    scheduleItemElements.size()
            );
            result.put(i, scheduleItemElements.subList(from, to));
            from = to;
        }
        return result;
    }

    private void returnScheduleMap(
            Map<Integer, List<Element>> mapItem,
            List<Element> scheduleTimeSheets,
            Map<Integer, Map<DayOfWeek, ScheduleDay>> scheduleDayMap,
            int weekType // 1 для нечётной, 2 для чётной
    ){
        for (var index = 0; index < scheduleTimeSheets.size(); index++) {
            List<Element> itemsForWeek = mapItem.get(index);

            if (itemsForWeek != null && !itemsForWeek.isEmpty()) {
                Map<DayOfWeek, ScheduleDay> existingScheduleDayMap = scheduleDayMap.get(weekType);

                Map<DayOfWeek, ScheduleDay> newScheduleDayMap = getSchedulesForWeek(
                        scheduleTimeSheets.get(index),
                        itemsForWeek
                );

                mergeScheduleDayMaps(existingScheduleDayMap, newScheduleDayMap);
            } else {
                log.warn("There are no schedule items for the week: {}", index);
            }
        }
    }
}
