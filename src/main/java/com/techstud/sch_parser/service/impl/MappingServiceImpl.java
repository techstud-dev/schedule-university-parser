package com.techstud.sch_parser.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuScheduleItem;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuTeacher;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.api.response.sseu.SseuLessonDay;
import com.techstud.sch_parser.model.api.response.sseu.SseuSubject;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuGroup;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuSchedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.*;
import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.*;

@Service
@Slf4j
public class MappingServiceImpl implements MappingService {
    @Override
    public Schedule mapSpbstuScheduleByScheduleDay(List<Document> documents) {
        Schedule scheduleSpbtstu = new Schedule();

        scheduleSpbtstu.setEvenWeekSchedule(new HashMap<>());
        scheduleSpbtstu.setOddWeekSchedule(new HashMap<>());

        Map<DayOfWeek, ScheduleDay> evenWeekLessons = returnScheduleDayListFromResponseSpbstu(documents.get(0));
        Map<DayOfWeek, ScheduleDay> oddWeekLessons = returnScheduleDayListFromResponseSpbstu(documents.get(1));

        scheduleSpbtstu.setEvenWeekSchedule(evenWeekLessons);
        scheduleSpbtstu.setOddWeekSchedule(oddWeekLessons);

        return scheduleSpbtstu;
    }

    @Override
    public Schedule mapMephiToSchedule(List<Document> documents) {
        log.info("Start mapping MEPHI data to schedule");

        if (documents.size() < 2) {
            throw new IllegalArgumentException("Not enought documents for even and odd weeks");
        }

        var evenWeekDocument = documents.get(0);
        var oddWeekDocument = documents.get(1);

        var evenWeekSchedule = getMephiWeekSchedule(evenWeekDocument);
        var oddWeekSchedule = getMephiWeekSchedule(oddWeekDocument);

        var schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        schedule.setSnapshotDate(new Date());

        log.info("MEPHI mapping {} completed", schedule);
        return schedule;
    }

    @Override
    public Schedule mapSseuToSchedule(List<SseuApiResponse> weekSseuSchedules) {
        log.info("Start mapping SSEU data to schedule");
        SseuApiResponse oddWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("ODD") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        SseuApiResponse evenWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("EVEN") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        Schedule schedule = new Schedule();

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = createSseuWeekScheduleWithoutLessons(evenWeekSseuSchedule);
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = createSseuWeekScheduleWithoutLessons(oddWeekSseuSchedule);

        evenWeekSchedule = fillSseuSchedule(evenWeekSseuSchedule, evenWeekSchedule);
        oddWeekSchedule = fillSseuSchedule(oddWeekSseuSchedule, oddWeekSchedule);

        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping SSEU data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapSsauToSchedule(List<Document> documents) {
        log.info("Start mapping SSAU data to schedule");
        Element evenElement = documents.get(0)
                .getElementsByClass("schedule")
                .first();

        Element oddElement = documents.get(1)
                .getElementsByClass("schedule")
                .first();

        if (evenElement == null && oddElement == null) {
            return null;
        }

        Map<DayOfWeek, ScheduleDay> evenSchedule = getSsauSchedule(evenElement);
        Map<DayOfWeek, ScheduleDay> oddSchedule = getSsauSchedule(oddElement);
        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenSchedule);
        schedule.setOddWeekSchedule(oddSchedule);
        schedule.setSnapshotDate(new Date());
        log.info("Mapping SSAU data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapBmstuToSchedule(BmstuApiResponse bmstuApiResponse) {
        log.info("Start mapping BMSTU data to schedule");
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();

        for (BmstuScheduleItem scheduleItem : bmstuApiResponse.getData().getSchedule()) {
            DayOfWeek dayOfWeek = DayOfWeek.of(scheduleItem.getDay());

            switch (scheduleItem.getWeek()) {
                case "all" -> {
                    addBmstuToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                    addBmstuToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
                }
                case "ch" -> addBmstuToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                case "zn" -> addBmstuToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
                default -> {
                }
            }
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping BMSTU data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapNsuToSchedule(Document document) {
        log.info("Start mapping NSU data to schedule");
        Schedule schedule = new Schedule();
        TimeSheet lastTimeSheet = null;

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();
        DayOfWeek[] daysOfWeek = DayOfWeek.values();

        for (DayOfWeek day : daysOfWeek) {
            evenWeekSchedule.put(day, new ScheduleDay());
            oddWeekSchedule.put(day, new ScheduleDay());
        }

        Elements rows = document.select("table.time-table tbody tr");

        for (Element row : rows) {
            lastTimeSheet = getNsuScheduleDay(row, evenWeekSchedule, oddWeekSchedule, daysOfWeek, lastTimeSheet);
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping NSU data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapUneconToSchedule(List<Document> documents) {
        log.info("Start mapping UNECON data to schedule");
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();

        for (Document document : documents) {
            if (document == null) {
                log.warn("Document is empty");
                continue;
            }

            Elements elements = document.select("table tbody tr");
            log.info("Found elements: {}", elements.size());

            DayOfWeek currentDayOfWeek;
            ScheduleDay evenWeekDay = null;
            ScheduleDay oddWeekDay = null;

            for (Element row : elements) {
                if (row.hasClass("new_day_border")) {
                    Element nextRow = row.nextElementSibling();
                    if (nextRow != null && nextRow.hasClass("new_day")) {
                        String dayText = nextRow.select(".day").text().trim();

                        try {
                            currentDayOfWeek = uneconParseDayOfWeek(dayText);
                            evenWeekDay = new ScheduleDay();
                            oddWeekDay = new ScheduleDay();
                            evenWeekSchedule.put(currentDayOfWeek, evenWeekDay);
                            oddWeekSchedule.put(currentDayOfWeek, oddWeekDay);
                        } catch (IllegalArgumentException e) {
                            log.error("Error while parsing day of week: {}", dayText, e);
                        }
                    }
                } else if (evenWeekDay != null && oddWeekDay != null) {
                    getUneconScheduleDay(row, evenWeekDay, oddWeekDay);
                }
            }
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping UNECON data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapPgupsToSchedule(List<Document> documents) {
        log.info("Start mapping PGUPS data to schedule");

        Schedule schedule = new Schedule();
        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(parsePgupsSchedule(documents.get(0)));
        schedule.setOddWeekSchedule(parsePgupsSchedule(documents.get(1)));

        log.info("Mapping PGUPS data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapMiitToSchedule(List<Document> documents) {
        log.info("Start mapping MIIT data to schedule");

        Schedule schedule = new Schedule();
        if (documents == null || documents.size() < 2) {
            log.error("Insufficient documents to parse even and odd week schedules");
            return schedule;
        }

        schedule.setEvenWeekSchedule(parseMiitWeekSchedule(documents.get(0)));
        schedule.setOddWeekSchedule(parseMiitWeekSchedule(documents.get(1)));

        log.info("Mapping MIIT data to schedule {} finished.", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> parseMiitWeekSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new EnumMap<>(DayOfWeek.class);
        Arrays.stream(DayOfWeek.values()).forEach(day -> weekSchedule.put(day, new ScheduleDay()));

        Elements rows = document.select("tr");
        if (rows.isEmpty()) {
            log.warn("No rows found in the document");
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
                            () -> log.warn("Could not parse day of week for text: {}", dayOfWeekText)
                    );
        }
        return dayOfWeekMapping;
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

    private void processMiitLessonRow(Element lessonRow, Map<DayOfWeek, ScheduleDay> weekSchedule,
                                      Map<Integer, DayOfWeek> dayOfWeekMapping) {
        Elements cells = lessonRow.select("td");
        TimeSheet timeSheet = getMiitTimeSheet(cells.first());

        if (timeSheet == null) {
            log.warn("Skipping row due to invalid time format");
            return;
        }

        for (int i = 1; i < cells.size(); i++) {
            Element lessonCell = cells.get(i);
            DayOfWeek dayOfWeek = dayOfWeekMapping.get(i - 1);

            if (dayOfWeek == null || lessonCell.text().isBlank()) {
                continue;
            }

            ScheduleObject scheduleObject = getMiitScheduleObject(lessonCell);
            weekSchedule.get(dayOfWeek)
                    .getLessons()
                    .get(timeSheet)
                    .add(scheduleObject);
        }
    }

    private TimeSheet getMiitTimeSheet(Element timeCell) {
        if (timeCell == null) {
            log.error("Time cell not found.");
            return null;
        }

        String timeText = Optional.ofNullable(timeCell.selectFirst(".timetable__grid-text_gray"))
                .map(Element::text)
                .orElse(null);
        if (timeText == null) {
            log.error("Time element with gray text not found in time cell.");
            return null;
        }

        String[] timeParts = timeText.split(" — ");
        return (timeParts.length == 2)
                ? new TimeSheet(timeParts[0].trim(), timeParts[1].trim())
                : null;
    }

    private ScheduleObject getMiitScheduleObject(Element lessonElement) {
        ScheduleObject scheduleObject = new ScheduleObject();

        Optional.ofNullable(lessonElement.selectFirst("div.timetable__grid-day-lesson"))
                .ifPresent(el -> scheduleObject.setName(el.ownText()));

        String typeText = lessonElement.select(".timetable__grid-text_gray").text().trim();
        scheduleObject.setType(mapMiitLessonTypeToScheduleType(typeText));

        Optional.ofNullable(lessonElement.selectFirst("a.icon-academic-cap"))
                .ifPresent(el -> scheduleObject.setTeacher(el.ownText()));

        scheduleObject.setGroups(lessonElement.select("span.icon-community, a.icon-community")
                .eachText());

        scheduleObject.setPlace(String.join(", ", lessonElement.select("a.icon-location").eachText()));

        return scheduleObject;
    }

    private Map<DayOfWeek, ScheduleDay> parsePgupsSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> schedule = new LinkedHashMap<>();
        Elements scheduleDays = document.getElementsByTag("tbody");

        scheduleDays.forEach(day -> {
            DayOfWeek dayOfWeek = parseDayOfWeek(day.getElementsByClass("kt-font-dark").text().toLowerCase());
            schedule.put(dayOfWeek, parsePgupsScheduleDay(day));
        });

        return schedule;
    }

    private ScheduleDay parsePgupsScheduleDay(Element dayElement) {
        Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

        dayElement.getElementsByTag("tr").forEach(lesson -> {
            String[] timeRange = lesson.getElementsByClass("text-center kt-shape-font-color-4")
                    .text()
                    .split("—");
            TimeSheet timeSheet = new TimeSheet(timeRange[0].trim(), timeRange[1].trim());
            lessons.put(timeSheet, parsePgupsScheduleObjects(lesson));
        });

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setLessons(lessons);
        return scheduleDay;
    }

    private List<ScheduleObject> parsePgupsScheduleObjects(Element lesson) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();

        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(lesson.getElementsByClass("mr-1").text().trim());
        scheduleObject.setPlace(lesson.select("a[href^=https://rasp.pgups.ru/schedule/room]").text().trim());
        scheduleObject.setType(ScheduleType.returnTypeByPgupsName(lesson.select("span[class^=badge]").text().trim()));
        scheduleObject.setTeacher(lesson.select("div > a[href^=https://rasp.pgups.ru/schedule/teacher].kt-link").text().trim());

        scheduleObjects.add(scheduleObject);
        return scheduleObjects;
    }

    private void getUneconScheduleDay(Element element, ScheduleDay evenWeekDay, ScheduleDay oddWeekDay) {
        TimeSheet timeSheet = parseUneconTimeSheet(element);
        if (timeSheet == null) {
            log.warn("Cant process timesheet in the element: {}", element);
            return;
        }

        Map<TimeSheet, List<ScheduleObject>> evenLessons = evenWeekDay.getLessons();
        Map<TimeSheet, List<ScheduleObject>> oddLessons = oddWeekDay.getLessons();

        evenLessons.computeIfAbsent(timeSheet, k -> new ArrayList<>()).addAll(parseUneconScheduleObject(element));
        oddLessons.computeIfAbsent(timeSheet, k -> new ArrayList<>()).addAll(parseUneconScheduleObject(element));
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

    private TimeSheet getNsuScheduleDay(Element row, Map<DayOfWeek, ScheduleDay> evenWeekSchedule,
                                        Map<DayOfWeek, ScheduleDay> oddWeekSchedule,
                                        DayOfWeek[] daysOfWeek, TimeSheet lastTimeSheet) {
        Element timeCell = row.select("td").first();
        if (timeCell != null) {
            String timeText = timeCell.text().trim();
            TimeSheet currentSheet = new TimeSheet(timeText);

            if (lastTimeSheet != null) {
                lastTimeSheet.setTo(currentSheet.getFrom());
            }

            if (row.nextElementSibling() == null) {
                LocalTime fromTime = currentSheet.getFrom();
                LocalTime toTime = fromTime.plusMinutes(110);
                currentSheet.setTo(LocalTime.parse(toTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
            }

            lastTimeSheet = currentSheet;

            for (DayOfWeek day : daysOfWeek) {
                evenWeekSchedule.get(day).getLessons()
                        .computeIfAbsent(currentSheet, k -> new ArrayList<>());
                oddWeekSchedule.get(day).getLessons()
                        .computeIfAbsent(currentSheet, k -> new ArrayList<>());
            }

            Elements dayCells = row.select("td:not(:first-child)");
            int dayIndex = 0;

            for (Element dayCell : dayCells) {
                Elements lessonCells = dayCell.select("div.cell");

                for (Element lessonCell : lessonCells) {
                    List<ScheduleObject> scheduleObjects = getNsuScheduleObjects(lessonCell);
                    String weekIndicator = lessonCell.select(".week").text().trim();

                    for (ScheduleObject scheduleObject : scheduleObjects) {
                        if (weekIndicator.isEmpty()) {
                            evenWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        } else if (weekIndicator.equals("Четная")) {
                            evenWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        } else if (weekIndicator.equals("Нечетная")) {
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        }
                    }
                }
                dayIndex++;
            }
        }
        return lastTimeSheet;
    }

    private List<ScheduleObject> getNsuScheduleObjects(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();
        Elements lessons = element.select("div.cell");

        for (Element lesson : lessons) {
            String type = lesson.select(".type").text().trim();
            String subject = lesson.select(".subject").text().trim();
            String teacher = lesson.select(".tutor").text().trim();
            String place = lesson.select(".room a").text().trim();

            ScheduleObject scheduleObject = new ScheduleObject();
            scheduleObject.setType(mapNsuLessonTypeToScheduleType(type));
            scheduleObject.setName(subject.isEmpty() ? null : subject);
            scheduleObject.setPlace(place.isEmpty() ? null : place);
            scheduleObject.setTeacher(teacher.isEmpty() ? null : teacher);

            scheduleObjects.add(scheduleObject);
        }

        return scheduleObjects;
    }

    @Override
    public Schedule mapTltsuToSchedule(List<TltsuApiResponse> response) {
        TltsuApiResponse oddSchedule = response.get(0);
        TltsuApiResponse evenSchedule = response.get(1);

        if (oddSchedule.getSchedules().isEmpty() && evenSchedule.getSchedules().isEmpty()) {
            return null;
        }
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        if (!oddSchedule.getSchedules().isEmpty()) {
            oddWeekSchedule = getWeekScheduleFromTltsu(oddSchedule);
        }

        if (!evenSchedule.getSchedules().isEmpty()) {
            evenWeekSchedule = getWeekScheduleFromTltsu(evenSchedule);
        }

        schedule.setOddWeekSchedule(oddWeekSchedule);
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> getWeekScheduleFromTltsu(TltsuApiResponse tltsuApiResponse) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        tltsuApiResponse.getSchedules().forEach(schedule -> {
            DayOfWeek currentDayOfWeek = getDayOfWeekTltsu(schedule);
            weekSchedule.put(currentDayOfWeek, addScheduleFromTltsu(weekSchedule.get(currentDayOfWeek), schedule));
        });

        return weekSchedule;
    }

    private ScheduleDay addScheduleFromTltsu(ScheduleDay scheduleDay, TltsuSchedule schedule) {
        if (scheduleDay == null) {
            scheduleDay = new ScheduleDay();
        }
        TimeSheet timeSheet = getSseuTimeSheet(schedule);
        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();

        if (lessons == null) {
            lessons = new LinkedHashMap<>();
        }
        List<ScheduleObject> scheduleObjects = lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>());
        scheduleObjects.add(getScheduleObjectFromTltsuSchedule(schedule));
        lessons.put(timeSheet, scheduleObjects);
        scheduleDay.setLessons(lessons);
        scheduleDay.setDate(parseTltsuDate(schedule.getDate()));
        return scheduleDay;
    }

    private ScheduleObject getScheduleObjectFromTltsuSchedule(TltsuSchedule schedule) {
        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(schedule.getDisciplineName());
        scheduleObject.setPlace(schedule.getClassroom().getName());
        if (schedule.getTeacher() != null) {
            scheduleObject.setTeacher(schedule.getTeacher().getLastName() + " " + schedule.getTeacher().getName() + schedule.getTeacher().getPatronymic());
        }
        scheduleObject.setGroups(schedule.getGroupsList()
                .stream()
                .map(TltsuGroup::getName)
                .toList());
        scheduleObject.setType(getScheduleTypeFromTltsuSchedule(schedule));
        return scheduleObject;
    }

    private ScheduleType getScheduleTypeFromTltsuSchedule(TltsuSchedule schedule) {
        if (schedule.getType() == null) {
            return ScheduleType.UNKNOWN;
        }
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лек", ScheduleType.LECTURE,
                "Пр", ScheduleType.PRACTICE,
                "СР", ScheduleType.INDEPENDENT_WORK,
                "ЛР", ScheduleType.LAB
        );
        return scheduleTypeMap.getOrDefault(schedule.getType(), ScheduleType.UNKNOWN);
    }

    private Date parseTltsuDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            log.warn("Error while parsing date {} TLTSU", date);
            return null;
        }
    }

    private TimeSheet getSseuTimeSheet(TltsuSchedule schedule) {
        Instant dateFrom = Instant.parse(schedule.getFromTime());
        Instant dateTo = Instant.parse(schedule.getToTime());
        LocalDateTime localDateTimeFrom = LocalDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
        LocalDateTime localDateTimeTo = LocalDateTime.ofInstant(dateTo, ZoneOffset.UTC);
        return new TimeSheet(localDateTimeFrom.toLocalTime(), localDateTimeTo.toLocalTime());
    }

    private DayOfWeek getDayOfWeekTltsu(TltsuSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate date = LocalDate.parse(schedule.getDate(), formatter);
        return date.getDayOfWeek();
    }

    private ScheduleDay getMephiScheduleDay(Element element) {
        var scheduleDayMap = element.select("div.list-group-item.d-xs-flex").stream()
                .collect(Collectors.toMap(
                        groupElement -> parseMephiTimeSheet(Objects.requireNonNull(groupElement.selectFirst(".lesson-time")).text()),
                        this::getMephiScheduleObjects,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        ScheduleDay scheduleDay = new ScheduleDay();
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

    private Map<DayOfWeek, ScheduleDay> getMephiWeekSchedule(Document document) {
        var weekSchedule = new LinkedHashMap<DayOfWeek, ScheduleDay>();
        var elements = document.select(".lesson-wday, .list-group");

        var currentDayOfWeek = new Object() { DayOfWeek value = null; };
        elements.forEach(element -> {
            if (element.hasClass("lesson-wday")) {
                currentDayOfWeek.value = parseDayOfWeek(element.text());
            } else if (element.hasClass("list-group") && currentDayOfWeek.value != null) {
                weekSchedule.put(currentDayOfWeek.value, getMephiScheduleDay(element));
            }
        });
        return weekSchedule;
    }

    private void addBmstuToSchedule(Map<DayOfWeek, ScheduleDay> weekSchedule, DayOfWeek dayOfWeek, BmstuScheduleItem scheduleItem) {
        ScheduleDay scheduleDay = weekSchedule.computeIfAbsent(dayOfWeek, k -> new ScheduleDay());
        addBmstuScheduleItemToDay(scheduleDay, scheduleItem);
    }

    private void addBmstuScheduleItemToDay(ScheduleDay scheduleDay, BmstuScheduleItem scheduleItem) {
        TimeSheet timeSheet = new TimeSheet(scheduleItem.getStartTime(), scheduleItem.getEndTime());
        ScheduleObject scheduleObject = createBmstuScheduleObject(scheduleItem);

        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();
        if (lessons == null) {
            lessons = new LinkedHashMap<>();
            scheduleDay.setLessons(lessons);
        }

        List<ScheduleObject> scheduleObjects = lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>());
        scheduleObjects.add(scheduleObject);
    }

    private ScheduleObject createBmstuScheduleObject(BmstuScheduleItem scheduleItem) {
        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(scheduleItem.getDiscipline().getFullName());

        List<String> groups = Arrays.stream(scheduleItem.getStream().split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
        scheduleObject.setGroups(groups);

        if (!scheduleItem.getAudiences().isEmpty()) {
            scheduleObject.setPlace(scheduleItem.getAudiences().get(0).getName());
        }

        if (!scheduleItem.getTeachers().isEmpty()) {
            BmstuTeacher teacher = scheduleItem.getTeachers().get(0);
            scheduleObject.setTeacher(String.format("%s %s %s",
                    teacher.getLastName(),
                    teacher.getFirstName(),
                    teacher.getMiddleName()).trim());
        }

        scheduleObject.setType(scheduleTypeByBmstuType(scheduleItem.getDiscipline().getActType()));
        return scheduleObject;
    }

    private ScheduleType scheduleTypeByBmstuType(String bmstuType) {
        if (bmstuType == null) {
            return ScheduleType.UNKNOWN;
        }

        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "lecture", ScheduleType.LECTURE,
                "seminar", ScheduleType.PRACTICE,
                "lab", ScheduleType.LAB,
                "pk", ScheduleType.UNKNOWN
        );

        return scheduleTypeMap.getOrDefault(bmstuType, ScheduleType.UNKNOWN);
    }

    private Map<DayOfWeek, ScheduleDay> getSsauSchedule(Element scheduleElement) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleDayMap.put(day, getSsauScheduleDay(day, scheduleElement));
        }
        return scheduleDayMap;
    }

    private ScheduleDay getSsauScheduleDay(DayOfWeek dayOfWeek, Element element) {
        Elements scheduleItemElements = element.getElementsByClass("schedule__item");
        List<Element> ssauTimeSheets = element.getElementsByClass("schedule__time");
        List<Element> ssauDayOfWeek = scheduleItemElements.subList(1, 7);
        List<Element> ssauLessons = scheduleItemElements.subList(7, scheduleItemElements.size());

        ScheduleDay scheduleDay = new ScheduleDay();
        Map<TimeSheet, List<ScheduleObject>> timeSheetListMap = new LinkedHashMap<>();

        for (int i = 0; i < ssauTimeSheets.size(); i++) {
            String startTime = ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(0).text();
            String endTime = ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(1).text();
            TimeSheet timeSheet = new TimeSheet(startTime, endTime);

            int currentElement = (i * ssauDayOfWeek.size()) + dayOfWeek.getValue() - 1;

            List<ScheduleObject> scheduleObjects;
            if (currentElement < ssauLessons.size()) {
                scheduleObjects = getSsauScheduleObject(ssauLessons.get(currentElement));
            } else {
                scheduleObjects = new ArrayList<>();
            }

            timeSheetListMap.put(timeSheet, scheduleObjects);
        }
        scheduleDay.setDateAsString(ssauDayOfWeek.get(1).getElementsByClass("schedule__head-date").text());
        scheduleDay.setLessons(timeSheetListMap);

        return scheduleDay;
    }

    private List<ScheduleObject> getSsauScheduleObject(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();
        List<Element> scheduleLessons = element.getElementsByClass("schedule__lesson");
        for (Element scheduleLesson : scheduleLessons) {
            ScheduleObject scheduleObject = new ScheduleObject();
            scheduleObject.setType(ScheduleType.returnTypeByRuName(scheduleLesson.getElementsByClass("schedule__lesson-type-chip").text()));
            scheduleObject.setName(scheduleLesson.getElementsByClass("schedule__discipline").text());
            scheduleObject.setPlace(scheduleLesson.getElementsByClass("schedule__place").text());
            scheduleObject.setTeacher(scheduleLesson.getElementsByClass("schedule__teacher").text());
            scheduleObject.setGroups(getSsauScheduleGroups(scheduleLesson.getElementsByClass("schedule__groups")));
            scheduleObjects.add(scheduleObject);
        }
        return scheduleObjects;
    }

    private List<String> getSsauScheduleGroups(Elements scheduleGroupsElement) {
        List<String> groupNames = new ArrayList<>();
        List<Element> aElements = scheduleGroupsElement.get(0).getElementsByTag("a");
        List<Element> spanElements = scheduleGroupsElement.get(0).getElementsByTag("span");
        if (aElements.isEmpty()) {
            for (Element spanElement : spanElements) {
                groupNames.add(spanElement.text());
            }
        } else {
            for (Element aElement : aElements) {
                groupNames.add(aElement.text());
            }
        }
        return groupNames;
    }

    private Map<DayOfWeek, ScheduleDay> createSseuWeekScheduleWithoutLessons(SseuApiResponse sseuSchedule) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        sseuSchedule.getHeaders().forEach(header -> {
            if (!header.getValue().equals("name")) {
                ScheduleDay scheduleDay = new ScheduleDay();
                scheduleDay.setDate(getSseuDate(header.getText()));
                weekSchedule.put(DayOfWeek.valueOf(header.getValue().toUpperCase()), scheduleDay);
            }
        });
        return weekSchedule;
    }

    private Date getSseuDate(String dateString) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEEE dd.MM.yyyy")
                .optionalStart()
                .appendLiteral('г')
                .optionalEnd()
                .optionalStart()
                .appendLiteral('.')
                .optionalEnd()
                .toFormatter(new Locale("ru"));
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    private Map<DayOfWeek, ScheduleDay> fillSseuSchedule(SseuApiResponse sseuSchedule, Map<DayOfWeek, ScheduleDay> weekSchedule) {
        for (int i = 0; i < sseuSchedule.getBody().size(); i++) {
            LocalTime from = LocalTime.parse(sseuSchedule.getBody().get(i).getName());

            int finalI = i;
            sseuSchedule.getBody().get(i).getDaySchedule().forEach((day, daySchedule) -> {
                if (weekSchedule.containsKey(DayOfWeek.valueOf(day.toUpperCase()))) {
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    Map<TimeSheet, List<ScheduleObject>> lessons = weekSchedule.get(dayOfWeek).getLessons();
                    TimeSheet timeSheet = getSseuTimeSheet(from, lessons.keySet());
                    if (timeSheet == null) {
                        timeSheet = new TimeSheet();
                        timeSheet.setFrom(from);
                        timeSheet.setTo(null);
                        lessons.put(timeSheet, new ArrayList<>());
                    }
                    List<ScheduleObject> scheduleObjects = lessons.get(timeSheet);
                    ScheduleObject addedScheduleObject = mapSseuLessonToScheduleObject(sseuSchedule.getBody().get(finalI).getDaySchedule().get(day));
                    assert addedScheduleObject != null;
                    if (addedScheduleObject.getName() != null && addedScheduleObject.getType() != null && addedScheduleObject.getPlace() != null) {
                        scheduleObjects.add(addedScheduleObject);
                    }
                }
            });
        }
        Map<DayOfWeek, ScheduleDay> result = new LinkedHashMap<>();
        weekSchedule.forEach((day, scheduleDay) -> {
            Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();
            List<TimeSheet> sortedTimeSheets = new ArrayList<>(scheduleDay.getLessons().keySet());
            sortedTimeSheets.sort(Comparator.comparing(TimeSheet::getFrom));

            TimeSheet previous = null;
            for (TimeSheet current : sortedTimeSheets) {
                if (previous != null) {
                    previous.setTo(current.getFrom());
                }
                lessons.put(current, scheduleDay.getLessons().get(current));
                previous = current;
            }
            if (previous != null) {
                previous.setTo(previous.getFrom().plusHours(1).plusMinutes(45));
            }

            ScheduleDay newScheduleDay = new ScheduleDay();
            newScheduleDay.setDate(scheduleDay.getDate());
            newScheduleDay.setLessons(lessons);
            result.put(day, newScheduleDay);
        });

        if (!result.containsKey(DayOfWeek.SUNDAY)) {
            Map<TimeSheet, List<ScheduleObject>> scheduleTimeSheetMap = result.get(DayOfWeek.SATURDAY).getLessons();
            scheduleTimeSheetMap.replaceAll((key, value) -> new ArrayList<>());
            ScheduleDay sunDayScheduleDay = new ScheduleDay();
            Date saturdayDate = result.get(DayOfWeek.SATURDAY).getDate();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(saturdayDate);
            calendar.add(Calendar.HOUR_OF_DAY, 24);
            sunDayScheduleDay.setDate(calendar.getTime());
            sunDayScheduleDay.setLessons(scheduleTimeSheetMap);
            result.put(DayOfWeek.SUNDAY, sunDayScheduleDay);
        }
        return result;
    }

    private ScheduleObject mapSseuLessonToScheduleObject(List<SseuLessonDay> daySchedule) {
        ScheduleObject scheduleObject = new ScheduleObject();
        if (daySchedule == null || daySchedule.isEmpty()) {
            return null;
        }

        SseuLessonDay lessonDay = daySchedule.get(0);

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getDiscipline() != null) {
            scheduleObject.setName(lessonDay.getWorkPlan().getDiscipline().getName());
        }

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getLessonTypes() != null) {
            scheduleObject.setType(mapSseuLessonTypeToScheduleType(lessonDay.getWorkPlan().getLessonTypes().getName()));
        }

        if (lessonDay.getSubject() != null && !lessonDay.getSubject().isEmpty()) {
            SseuSubject subject = lessonDay.getSubject().get(0);

            if (subject.getName() != null) {
                scheduleObject.setTeacher(subject.getName());
            }

            if (subject.getAudiences() != null && !subject.getAudiences().isEmpty()) {
                scheduleObject.setPlace(subject.getAudiences().get(0).getItemName());
            }
        }

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getGroup() != null) {
            String groupName = lessonDay.getWorkPlan().getGroup().getName();
            if (!scheduleObject.getGroups().contains(groupName)) {
                scheduleObject.getGroups().add(groupName);
            }
        }
        return scheduleObject;
    }

    private TimeSheet getSseuTimeSheet(LocalTime from, Set<TimeSheet> keySet) {
        return keySet.stream()
                .filter(timeSheet -> timeSheet.getFrom().equals(from))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param actualWeekList актуальный спсиок дней недели в результате парсинга
     * @return List<DayOfWeek> возвращаемый список с недостающими днями недели
     */
    private List<DayOfWeek> returnMissingDayOfTheWeek(List<DayOfWeek> actualWeekList){
        List<DayOfWeek> weekListConstant = IntStream
                .rangeClosed(1, 7)
                .mapToObj(DayOfWeek::of)
                .toList();

        return weekListConstant.stream()
                .filter(element -> !actualWeekList.contains(element))
                .toList();
    }

    /**
     * @param elementsHTMLResponse входной список элементов парсинга
     * @return JsonNode возвращаемый узел json документа
     */
    private JsonNode returnJsonNodeFromResponseSpbstu(Elements elementsHTMLResponse){
        return elementsHTMLResponse.stream()
                .map(element -> element.html().trim())
                .filter(scriptContent -> scriptContent.startsWith("window.__INITIAL_STATE__ ="))
                .findFirst()
                .map(element -> {
                    try{
                        return new ObjectMapper().readTree(element
                                .replace("window.__INITIAL_STATE__ =", "")
                                .replaceAll(";$", "")
                                .trim()).path("lessons").path("data");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).orElseThrow(() -> new RuntimeException("Null response body from API."));
    }

    /**
     * @param responseFromHTML входной узел json документа из ответа API
     * @return List<ScheduleDay> возвращаемый список SD
     */
    private List<ScheduleDay> returnObjectParsingListFromResponseSpbstu(JsonNode responseFromHTML){
        List<ScheduleDay> objectParsing = new LinkedList<>();
        responseFromHTML.fields().forEachRemaining(field ->
            field.getValue().forEach(day -> {
                ScheduleDay scheduleDay = new ScheduleDay();
                Map<TimeSheet, List<ScheduleObject>> mappedStruct = new LinkedHashMap<>();
                String dateForObject = day.path("date").asText();
                JsonNode lessonArray = day.path("lessons");
                lessonArray.forEach(lesson -> {
                    TimeSheet localTimeSheet = new TimeSheet(lesson.path("time_start").asText(), lesson.path("time_end").asText());
                    ScheduleObject scheduleObject = new ScheduleObject();
                    scheduleObject.setType(returnScheduleTypeSpbstu(
                            lesson.path("typeObj").path("name").asText()
                    ));
                    scheduleObject.setName(lesson.path("subject").asText());
                    lesson.path("teachers").forEach(teacher -> scheduleObject.setTeacher(teacher.path("full_name").asText()));
                    lesson.path("auditories").forEach(auditor -> scheduleObject.setPlace(auditor.path("name").asText()));
                    List<String> groups = new ArrayList<>();
                    lesson.path("groups").forEach(group -> groups.add(group.path("name").asText()));
                    scheduleObject.setGroups(groups);
                    mappedStruct.computeIfAbsent(localTimeSheet, k -> new ArrayList<>()).add(scheduleObject);
                });
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date = formatter.parse(dateForObject);
                    scheduleDay.setDate(date);
                } catch (ParseException e) {
                    log.error(e.getMessage());
                }
                scheduleDay.setLessons(mappedStruct);
                objectParsing.add(scheduleDay);
            })
        );
        return objectParsing;
    }

    /**
     * @param documentResponse html ответ от API
     * @return Map<DayOfWeek, ScheduleDay> структура возвращающая день недели и экземпляр учебного дня
     */
    private Map<DayOfWeek, ScheduleDay> returnScheduleDayListFromResponseSpbstu(Document documentResponse){
        Map<DayOfWeek, ScheduleDay> weekScheduleDayMap = new LinkedHashMap<>();
        List<DayOfWeek> actualWeekList = new LinkedList<>();

        returnObjectParsingListFromResponseSpbstu(
                returnJsonNodeFromResponseSpbstu(documentResponse.select("script"))
        ).forEach(data -> {
            try{
                DayOfWeek dayOfWeek = data
                        .getDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .getDayOfWeek();
                weekScheduleDayMap.put(dayOfWeek, data);
                actualWeekList.add(dayOfWeek);
            } catch(Exception e){
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        });

        returnMissingDayOfTheWeek(actualWeekList)
                .forEach(day -> weekScheduleDayMap.put(day, new ScheduleDay()));

        return weekScheduleDayMap;
    }
}
