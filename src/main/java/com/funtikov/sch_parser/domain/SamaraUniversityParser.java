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

        // Попробуйте обращаться к "https://ssau.ru/rasp?groupId=531052816&selectedWeek=2&selectedWeekday=1" запуская тест
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
        // Мапа, где Integer - ключ, номер недели (1 - нечётная, 2 - чётная)
        Map<Integer, Map<DayOfWeek, ScheduleDay>> scheduleDayMap = new LinkedHashMap<>();

        // Получаем первый элемент с классом "schedule" для чётной и нечётной недель
        Element evenElement = evenDoc.getElementsByClass("schedule").first();
        Element oddElement = oddDoc.getElementsByClass("schedule").first();

        // Инициализация мап для нечётной (1) и чётной (2) недель
        scheduleDayMap.put(1, new HashMap<>());
        scheduleDayMap.put(2, new HashMap<>());

        if (evenElement == null) {
            log.error("Не удалось найти элемент с классом 'schedule' на странице четного расписания.");
            return scheduleDayMap;
        }

        // Получаем список всех элементов с классом "schedule__item"
        List<Element> scheduleItemElements = evenElement
                .getElementsByClass("schedule__item")
                .stream()
                .toList();

        if (scheduleItemElements.size() < 8) {
            log.warn("Недостаточно элементов расписания: {}", scheduleItemElements.size());
            return scheduleDayMap;
        }

        scheduleItemElements = scheduleItemElements.subList(7, scheduleItemElements.size());

        // Получаем элементы с временными интервалами
        List<Element> scheduleTimeSheets = evenElement
                .getElementsByClass("schedule__time")
                .stream()
                .toList();

        // Проверяем, есть ли временные интервалы в расписании
        if (scheduleTimeSheets.isEmpty()) {
            log.warn("Нет временных элементов расписания для парсинга.");
            return scheduleDayMap;
        }

        // FIXME: Мне кажется, что все проблемы находятся тут (?)

        // Рассчитываем, сколько элементов расписания относится к каждому временному интервалу
        // (На самом деле, до сих пор немного не понимаю эту логику, по-моему, она не работает xd (?))
        int chunkCount = (int) Math.ceil((double) scheduleItemElements.size() / (double) scheduleTimeSheets.size());

        // Создаём карту для хранения чанков элементов расписания, привязанных к каждому временному интервалу
        Map<Integer, List<Element>> scheduleItemElementsChunk = new LinkedHashMap<>();
        int from = 0;

        // Разбиваем элементы расписания по временным интервалам с помощью цикла
        for (int i = 0; i < chunkCount; i++) {
            int to = Math.min(from + (scheduleItemElements.size() / scheduleTimeSheets.size()), scheduleItemElements.size());
            scheduleItemElementsChunk.put(i, scheduleItemElements.subList(from, to));
            from = to;
        }

        // Проходим по каждому временному интервалу и добавляем соответствующие элементы расписания
        for (int scheduleTimeSheet = 0; scheduleTimeSheet < scheduleTimeSheets.size(); scheduleTimeSheet++) {
            List<Element> itemsForWeek = scheduleItemElementsChunk.get(scheduleTimeSheet);

            // Если есть элементы расписания для текущей недели, парсим их
            if (itemsForWeek != null && !itemsForWeek.isEmpty()) {
                scheduleDayMap.put(2,
                        getSchedulesForWeek(scheduleTimeSheets.get(scheduleTimeSheet), itemsForWeek));
            } else {
                log.warn("Нет элементов расписания для недели: {}", scheduleTimeSheet);
            }
        }
        return scheduleDayMap;
    }

    private Map<DayOfWeek, ScheduleDay> getSchedulesForWeek(Element timeSheetElement, List<Element> scheduleItemElements) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();

        // Проверка: если список пустой - то возвращаем его пустым, а не пытаемся идти дальше
        if (scheduleItemElements.isEmpty()) {
            log.warn("Нет элементов расписания для парсинга.");
            return scheduleDayMap;
        }

        // Проходимся по каждому элементу расписания
        for (int i = 0; i < scheduleItemElements.size(); i++) {
            Element scheduleItem = scheduleItemElements.get(i);
            TimeSheet timeSheet = parseTimeSheet(timeSheetElement);

            List<ScheduleObject> lessons = getScheduleInfo(scheduleItem);

            ScheduleDay scheduleDay = new ScheduleDay();
            Map<TimeSheet, List<ScheduleObject>> dayLessons = new LinkedHashMap<>();
            dayLessons.put(timeSheet, lessons);
            scheduleDay.setLessons(dayLessons);

            // Это соотносит день недели с расписание, "(i + 1), scheduleDay)" преобразует индекс в день недели
            scheduleDayMap.put(DayOfWeek.of(i + 1), scheduleDay);
        }

        return scheduleDayMap;
    }

    /**
     *   Подумал, что лучше вынести логику парсинга времени в отдельный метод,
     *   а не пытаться парсить всё в одном. Так кажется проще
     */

    public TimeSheet parseTimeSheet(Element timeSheetElement) {
        List<String> timeSheetAttributes =
                timeSheetElement.getElementsByClass("schedule__time-item")
                        .stream().map(Element::text).toList();

        // Проверка: если в переменной timeSheetAttributes меньше 2 значений - то выкидываем exception
        if (timeSheetAttributes.size() < 2) {
            log.warn("Недостаточно временных элементов для парсинга: {}", timeSheetAttributes);
            throw new IllegalArgumentException("Недостаточно временных элементов для парсинга.");
        }

        // Получаем первый и второй атрибуты FROM и TO
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

        // Складывает "null", если в переменных нету значений
        scheduleObject.setName(lessonName.isEmpty() ? null : lessonName);
        scheduleObject.setPlace(lessonPlace.isEmpty() ? null : lessonPlace);
        scheduleObject.setTeacher(lessonTeacher.isEmpty() ? null : lessonTeacher);
        scheduleObject.setGroups(lessonGroups);

        try {
            scheduleObject.setType(returnTypeByRuName(lessonType.trim()));
        } catch (IllegalArgumentException e) {
            log.error("Illegal lesson type = {}", lessonType, e);
        }

        return List.of(scheduleObject);
    }
}
