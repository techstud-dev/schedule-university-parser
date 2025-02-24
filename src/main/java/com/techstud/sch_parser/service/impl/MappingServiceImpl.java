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
import java.time.temporal.TemporalAdjusters;
import java.util.*;
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
                    if (addedScheduleObject.getName() != null && addedScheduleObject.getType() != null) {
                        if(addedScheduleObject.getPlace() == null){
                            addedScheduleObject.setPlace("No audience specified");
                        }
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
