package com.techstud.sch_parser.service.impl;

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

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.*;

@Service
@Slf4j
public class MappingServiceImpl implements MappingService {

    @Override
    public Schedule mapMephiToSchedule(List<Document> documents) {
        log.info("Start mapping MEPHI data to schedule");
        if (documents.size() < 2) {
            throw new IllegalArgumentException("Not enough elements for even and odd documents");
        }

        Document evenWeekDocument = documents.get(0);
        Document oddWeekDocument = documents.get(1);

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = getMephiWeekSchedule(evenWeekDocument);
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = getMephiWeekSchedule(oddWeekDocument);

        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        schedule.setSnapshotDate(new Date());

        log.info("Mapping MEPHI data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapSseuToSchedule(List<SseuApiResponse> weekSseuSchedules) {
        log.info("Start mapping SSEU data to schedule");
        SseuApiResponse oddWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("ODD") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        SseuApiResponse evenWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("EVEN") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        Schedule schedule = new Schedule();

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = createWeekScheduleWithoutLessons(evenWeekSseuSchedule);
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = createWeekScheduleWithoutLessons(oddWeekSseuSchedule);

        evenWeekSchedule = fillSchedule(evenWeekSseuSchedule, evenWeekSchedule);
        oddWeekSchedule = fillSchedule(oddWeekSseuSchedule, oddWeekSchedule);

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
                    addToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                    addToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
                }
                case "ch" -> addToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                case "zn" -> addToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
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
                            currentDayOfWeek = staticUneconParseDayOfWeek(dayText);
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
        schedule.setEvenWeekSchedule(parseSchedule(documents.get(0)));
        schedule.setOddWeekSchedule(parseSchedule(documents.get(1)));

        log.info("Mapping PGUPS data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapMiitToSchedule(List<Document> documents) {
        log.info("Start mapping MIIT data to schedule");

        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(new HashMap<>());
        schedule.setOddWeekSchedule(new HashMap<>());

        log.info("Mapping even week schedule.");
        Map<DayOfWeek, ScheduleDay> evenWeekLessons = parseMiitWeekSchedule(documents.get(0));
        schedule.setEvenWeekSchedule(evenWeekLessons);

        log.info("Mapping odd week schedule.");
        Map<DayOfWeek, ScheduleDay> oddWeekLessons = parseMiitWeekSchedule(documents.get(1));
        schedule.setOddWeekSchedule(oddWeekLessons);

        log.info("Mapping MIIT data to schedule finished.");
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> parseMiitWeekSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        Elements rows = document.select("tr");

        if (rows.isEmpty()) {
            log.warn("No rows found in the document");
            return weekSchedule;
        }

        Element headerRow = rows.get(0);
        Elements dayHeaders = headerRow.select("th");
        Map<Integer, DayOfWeek> dayOfWeekMapping = new LinkedHashMap<>();

        for (int i = 1; i < dayHeaders.size(); i++) {
            String dayOfWeekText = dayHeaders.get(i).text().split(" ")[0].trim();
            DayOfWeek dayOfWeek = staticMiitParseDayOfWeeK(dayOfWeekText);
            if (dayOfWeek != null) {
                dayOfWeekMapping.put(i - 1, dayOfWeek);
                weekSchedule.put(dayOfWeek, new ScheduleDay());
                log.info("Mapped column index {} to {}", i - 1, dayOfWeek);
            } else {
                log.warn("Could not parse day of week for text: {}", dayOfWeekText);
            }
        }

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            Element lessonRow = rows.get(rowIndex);
            Elements cells = lessonRow.select("td");

            if (cells.isEmpty()) {
                log.warn("Empty row encountered at index {}", rowIndex);
                continue;
            }

            Element timeCell = cells.get(0);
            TimeSheet timeSheet = getMiitTimeSheet(timeCell);

            for (int i = 1; i < cells.size(); i++) {
                Element lessonCell = cells.get(i);
                DayOfWeek dayOfWeek = dayOfWeekMapping.get(i - 1);

                if (dayOfWeek == null) {
                    log.warn("No corresponding day for column index {}", i - 1);
                    continue;
                }

                // Пропускаем пустые ячейки
                if (lessonCell.text().trim().isEmpty()) {
                    log.info("Empty cell for day {} at time {}, skipping.", dayOfWeek, timeSheet);
                    continue;
                }

                log.info("Mapping lessons for day: {}", dayOfWeek);

                ScheduleObject scheduleObject = getMiitScheduleObject(lessonCell);

                log.info("Parsed schedule object: {}", scheduleObject);

                ScheduleDay scheduleDay = weekSchedule.get(dayOfWeek);
                scheduleDay.getLessons().computeIfAbsent(timeSheet, k -> new ArrayList<>()).add(scheduleObject);
            }
        }

        return weekSchedule;
    }

    private TimeSheet getMiitTimeSheet(Element timeCell) {
        if (timeCell == null) {
            log.error("Time cell not found.");
            return null;
        }

        String timeText = timeCell.selectFirst(".timetable__grid-text_gray").text();
        log.info("Processing time text: '{}'", timeText);

        String[] timeParts = timeText.split(" — ");
        if (timeParts.length < 2) {
            log.error("Invalid time format for time cell: '{}'", timeText);
            return null;
        }

        return new TimeSheet(timeParts[0].trim(), timeParts[1].trim());
    }

    private ScheduleObject getMiitScheduleObject(Element lessonElement) {
        ScheduleObject scheduleObject = new ScheduleObject();

        Element lessonName = lessonElement.select("div.timetable__grid-day-lesson").first();
        if (lessonName != null) {
            String lessonNameText = lessonName.ownText();
            scheduleObject.setName(lessonNameText);
        } else {
            scheduleObject.setName(null);
        }

        String type = lessonElement.select(".timetable__grid-text_gray").text();
        if (type != null) {
            scheduleObject.setType(mapMiitLessonTypeToScheduleType(type));
        } else {
            scheduleObject.setType(null);
        }

        Element teacher = lessonElement.select("a.icon-academic-cap").first();
        if (teacher != null) {
            String teacherText = teacher.ownText();
            scheduleObject.setTeacher(teacherText);
        } else {
            scheduleObject.setTeacher(null);
        }

        Elements groupElements = lessonElement.select("span.icon-community, a.icon-community");
        if (groupElements != null) {
            for (Element groupElementEach : groupElements) {
                scheduleObject.getGroups().add(groupElementEach.ownText());
            }
        } else {
            scheduleObject.setGroups(null);
        }

        Elements placeElements = lessonElement.select("a.icon-location");
        StringBuilder allPlaces = new StringBuilder();
        for (Element placeElementEach : placeElements) {
            String placeElementText = placeElementEach.ownText();
            if (!placeElementText.isEmpty()) {
                if (!allPlaces.isEmpty()) {
                    allPlaces.append(", ");
                }
                allPlaces.append(placeElementText);
            }
        }

        scheduleObject.setPlace(allPlaces.toString());

        return scheduleObject;
    }

    private Map<DayOfWeek, ScheduleDay> parseSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> schedule = new LinkedHashMap<>();
        Elements scheduleDays = document.getElementsByTag("tbody");

        scheduleDays.forEach(day -> {
            DayOfWeek dayOfWeek = staticParseDayOfWeek(day.getElementsByClass("kt-font-dark").text().toLowerCase());
            schedule.put(dayOfWeek, parseScheduleDay(day));
        });

        return schedule;
    }

    private ScheduleDay parseScheduleDay(Element dayElement) {
        Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

        dayElement.getElementsByTag("tr").forEach(lesson -> {
            String[] timeRange = lesson.getElementsByClass("text-center kt-shape-font-color-4")
                    .text()
                    .split("—");
            TimeSheet timeSheet = new TimeSheet(timeRange[0].trim(), timeRange[1].trim());
            lessons.put(timeSheet, parseScheduleObjects(lesson));
        });

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setLessons(lessons);
        return scheduleDay;
    }

    private List<ScheduleObject> parseScheduleObjects(Element lesson) {
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

        evenLessons.computeIfAbsent(timeSheet, k -> new ArrayList<>()).addAll(parseScheduleObject(element));
        oddLessons.computeIfAbsent(timeSheet, k -> new ArrayList<>()).addAll(parseScheduleObject(element));
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

    private List<ScheduleObject> parseScheduleObject(Element element) {
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

            lastTimeSheet = currentSheet;

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
                                    .computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(scheduleObject);
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(scheduleObject);
                        } else if (weekIndicator.equals("Четная")) {
                            evenWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(scheduleObject);
                        } else if (weekIndicator.equals("Нечетная")) {
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(scheduleObject);
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

    private ScheduleDay getMephiScheduleDay(Element element) {
        List<Element> groupList = element.select("div.list-group-item.d-xs-flex");

        ScheduleDay scheduleDay = new ScheduleDay();
        Map<TimeSheet, List<ScheduleObject>> scheduleDayMap = new LinkedHashMap<>();

        for (Element groupElement : groupList) {
            Elements timeElements = groupElement.select(".lesson-time");

            String timeRange = Objects.requireNonNull(timeElements.first()).text();
            TimeSheet timeSheet = parseMephiTimeSheet(timeRange);

            List<ScheduleObject> scheduleObjects = getMephiScheduleObjects(groupElement);
            scheduleDayMap.put(timeSheet, scheduleObjects);
        }

        scheduleDay.setLessons(scheduleDayMap);
        return scheduleDay;
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
        TimeSheet timeSheet = getTimeSheet(schedule);
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

    private TimeSheet getTimeSheet(TltsuSchedule schedule) {
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

    private List<ScheduleObject> getMephiScheduleObjects(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();

        Elements lessonElements = element.select(".lesson-lessons > .lesson");

        for (Element lessonElement : lessonElements) {
            ScheduleObject scheduleObject = new ScheduleObject();

            String fullText = lessonElement.ownText().trim();
            String[] ownTextParts = fullText.split("Подгруппа");
            String lessonName = ownTextParts[0].trim();

            lessonName = lessonName.replaceAll("[,\\s]+$", "");

            List<String> groups = new ArrayList<>();
            if (ownTextParts.length > 1) {
                groups.add("Подгруппа " + ownTextParts[1].trim());
            }

            String place = null;
            Element placeElement = lessonElement.selectFirst("i.fa-map-marker + a.text-nowrap");
            if (placeElement != null) {
                place = placeElement.text().trim();
            }

            if (place == null) {
                Element altPlaceElement = lessonElement.selectFirst("span.label.label-purple");
                if (altPlaceElement != null) {
                    place = altPlaceElement.text().trim();
                }
            }

            Elements teacherElements = lessonElement.select("span.text-nowrap");
            List<String> teachers = new ArrayList<>();
            for (Element teacherElement : teacherElements) {
                Element linkElement = teacherElement.selectFirst("a");
                if (linkElement != null) {
                    teachers.add(linkElement.text().trim());
                }
            }

            String type = lessonElement.select(".label-lesson").text().trim();

            if (type.isEmpty()) {
                Element titleElement = lessonElement.selectFirst(".title");
                if (titleElement != null && titleElement.text().contains("Резерв"))
                    type = "Резерв";
                else
                    type = "UNKNOWN";
            }

            scheduleObject.setType(mapMephiLessonTypeToScheduleType(type));
            scheduleObject.setName(lessonName);
            scheduleObject.setTeacher(teachers.isEmpty() ? null : String.join(", ", teachers));
            scheduleObject.setPlace(place);
            scheduleObject.setGroups(groups);

            scheduleObjects.add(scheduleObject);
        }

        return scheduleObjects;
    }

    private TimeSheet parseMephiTimeSheet(String timeRange) {
        String[] times = timeRange.split("—");
        if (times.length == 2) {
            String fromTime = times[0].trim();
            String toTime = times[1].trim();
            return new TimeSheet(fromTime, toTime);
        }

        return null;
    }

    private Map<DayOfWeek, ScheduleDay> getMephiWeekSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        DayOfWeek currentDayOfWeek = null;

        Elements elements = document.select(".lesson-wday, .list-group");

        for (Element element : elements) {
            if (element.hasClass("lesson-wday")) {
                String dayOfWeekText = element.text();
                currentDayOfWeek = staticParseDayOfWeek(dayOfWeekText);
            } else if (element.hasClass("list-group") && currentDayOfWeek != null) {
                ScheduleDay scheduleDay = getMephiScheduleDay(element);
                weekSchedule.put(currentDayOfWeek, scheduleDay);
            }
        }

        return weekSchedule;
    }

    private void addToSchedule(Map<DayOfWeek, ScheduleDay> weekSchedule, DayOfWeek dayOfWeek, BmstuScheduleItem scheduleItem) {
        ScheduleDay scheduleDay = weekSchedule.computeIfAbsent(dayOfWeek, k -> new ScheduleDay());
        addScheduleItemToDay(scheduleDay, scheduleItem);
    }

    private void addScheduleItemToDay(ScheduleDay scheduleDay, BmstuScheduleItem scheduleItem) {
        TimeSheet timeSheet = new TimeSheet(scheduleItem.getStartTime(), scheduleItem.getEndTime());
        ScheduleObject scheduleObject = createScheduleObject(scheduleItem);

        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();
        if (lessons == null) {
            lessons = new LinkedHashMap<>();
            scheduleDay.setLessons(lessons);
        }

        List<ScheduleObject> scheduleObjects = lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>());
        scheduleObjects.add(scheduleObject);
    }

    private ScheduleObject createScheduleObject(BmstuScheduleItem scheduleItem) {
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
            if (dayOfWeek.equals(DayOfWeek.SUNDAY)) {
                timeSheetListMap.put(new TimeSheet(ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(0).text(),
                        ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(1).text()), new ArrayList<>());
                continue;
            }
            int currentElement = (i * ssauDayOfWeek.size() + dayOfWeek.getValue() - 1);
            try {
                timeSheetListMap.put(new TimeSheet(ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(0).text(),
                        ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(1).text()), getSsauScheduleObject(ssauLessons.get(currentElement)));
            } catch (IndexOutOfBoundsException e) {
                break;
            }
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

    private Map<DayOfWeek, ScheduleDay> createWeekScheduleWithoutLessons(SseuApiResponse sseuSchedule) {
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

    private Map<DayOfWeek, ScheduleDay> fillSchedule(SseuApiResponse sseuSchedule, Map<DayOfWeek, ScheduleDay> weekSchedule) {
        for (int i = 0; i < sseuSchedule.getBody().size(); i++) {
            LocalTime from = LocalTime.parse(sseuSchedule.getBody().get(i).getName());

            int finalI = i;
            sseuSchedule.getBody().get(i).getDaySchedule().forEach((day, daySchedule) -> {
                if (weekSchedule.containsKey(DayOfWeek.valueOf(day.toUpperCase()))) {
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    Map<TimeSheet, List<ScheduleObject>> lessons = weekSchedule.get(dayOfWeek).getLessons();
                    TimeSheet timeSheet = getTimeSheet(from, lessons.keySet());
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

    private TimeSheet getTimeSheet(LocalTime from, Set<TimeSheet> keySet) {
        return keySet.stream()
                .filter(timeSheet -> timeSheet.getFrom().equals(from))
                .findFirst()
                .orElse(null);
    }

    private ScheduleType mapSseuLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лекции", ScheduleType.LECTURE,
                "Практические", ScheduleType.PRACTICE,
                "Лабораторные", ScheduleType.LAB,
                "Пересдача Зачет", ScheduleType.EXAM,
                "Пересдача Экзамен", ScheduleType.EXAM);
        return scheduleTypeMap.get(lessonType);
    }

    private ScheduleType mapMephiLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лек", ScheduleType.LECTURE,
                "Пр", ScheduleType.PRACTICE,
                "Лаб", ScheduleType.LAB,
                "Резерв", ScheduleType.UNKNOWN
        );
        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }

    private ScheduleType mapNsuLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "пр", ScheduleType.PRACTICE,
                "лек", ScheduleType.LECTURE,
                "лаб", ScheduleType.LAB);
        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }

    private ScheduleType mapMiitLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Практическое занятие", ScheduleType.PRACTICE,
                "Лекция", ScheduleType.LECTURE,
                "Лабораторная работа", ScheduleType.LAB);
        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }
}
