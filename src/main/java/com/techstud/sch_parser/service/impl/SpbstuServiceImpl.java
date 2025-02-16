package com.techstud.sch_parser.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.ScheduleObject;
import com.techstud.sch_parser.model.TimeSheet;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.IntStream;

import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.returnScheduleTypeSpbstu;

@Service
@Slf4j
public class SpbstuServiceImpl implements MappingServiceRef<List<Document>> {

    /**
     * @param source incoming list of html pages documents
     * @return Schedule current final schedule
     * @throws EmptyScheduleException error returning an empty schedule
     */
    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        Schedule scheduleSpbtstu = new Schedule();

        scheduleSpbtstu.setEvenWeekSchedule(new HashMap<>());
        scheduleSpbtstu.setOddWeekSchedule(new HashMap<>());

        Map<DayOfWeek, ScheduleDay> evenWeekLessons = returnScheduleDayListFromResponseSpbstu(source.get(0));
        Map<DayOfWeek, ScheduleDay> oddWeekLessons = returnScheduleDayListFromResponseSpbstu(source.get(1));

        scheduleSpbtstu.setEvenWeekSchedule(evenWeekLessons);
        scheduleSpbtstu.setOddWeekSchedule(oddWeekLessons);

        return scheduleSpbtstu;
    }

    /**
     * @param documentResponse html answer from the api
     * @return Map<DayOfWeek, ScheduleDay> structure returning day of the week and copy of the school day
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

    /**
     * @param responseFromHTML json input node of the api response
     * @return List<ScheduleDay> returned sd list
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
     * @param elementsHTMLResponse input list of parsing elements
     * @return JsonNode returned json node of a document
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
     * @param actualWeekList actual list of days of the week as a result of parsing
     * @return List<DayOfWeek> returned list with missing days of the week
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
}
