package com.techstud.sch_parser.model.api.response.mapping;

import java.util.List;

public record LessonDto(
        String isEvenWeek,
        String time,
        String type,
        String name,
        String teacher,
        String place,
        List<String> groups
) {}