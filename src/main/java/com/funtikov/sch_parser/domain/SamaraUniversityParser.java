package com.funtikov.sch_parser.domain;

import com.funtikov.sch_parser.model.Schedule;
import com.funtikov.sch_parser.model.ScheduleDay;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

public class SamaraUniversityParser implements Parser {

    @Override
    public Schedule parserSchedule(String url) throws IOException {
        Document doc = Jsoup
                .connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .get();

        Element htmlClassSchedule = doc.getElementsByClass("schedule").first();
        Map<DayOfWeek, ScheduleDay> scheduleEvenMap = getSchedulesSkeleton();
        Map<DayOfWeek, ScheduleDay> scheduleOddMap = getSchedulesSkeleton();

        fillScheduleDayMap(htmlClassSchedule, scheduleEvenMap);
    }

    private Map<DayOfWeek, ScheduleDay> getSchedulesSkeleton() {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new HashMap<>();
        scheduleDayMap.put(DayOfWeek.MONDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.TUESDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.WEDNESDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.THURSDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.FRIDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.SATURDAY, new ScheduleDay());
        scheduleDayMap.put(DayOfWeek.SUNDAY, new ScheduleDay());
        return scheduleDayMap;
    }

    private void fillScheduleDayMap(Element htmlClassSchedule, Map<DayOfWeek, ScheduleDay> scheduleDayMap) {

    }
}
