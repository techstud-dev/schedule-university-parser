package com.techstud.sch_parser.service;

import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import org.jsoup.nodes.Document;

import java.util.List;

public interface MappingService {
    Schedule mapSseuToSchedule (List<SseuApiResponse> weekSseuSchedules);
    Schedule mapSsauToSchedule (List<Document> documents);
    Schedule mapMephiToSchedule (List<Document> documents);
    Schedule mapBmstuToSchedule (BmstuApiResponse bmstuApiResponse);
    Schedule mapNsuToSchedule(Document document, boolean isEvenWeek);
}
