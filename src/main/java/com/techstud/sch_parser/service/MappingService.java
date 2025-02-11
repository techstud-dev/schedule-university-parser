package com.techstud.sch_parser.service;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import org.jsoup.nodes.Document;

import java.util.List;

public interface MappingService {
    Schedule mapSseuToSchedule(List<SseuApiResponse> weekSseuSchedules);

    Schedule mapSsauToSchedule(List<Document> documents);

    Schedule mapBmstuToSchedule(BmstuApiResponse bmstuApiResponse);

    Schedule mapNsuToSchedule(Document document);

    Schedule mapUneconToSchedule(List<Document> documents);

    Schedule mapTltsuToSchedule(List<TltsuApiResponse> documents) throws EmptyScheduleException;

    Schedule mapPgupsToSchedule(List<Document> documents);

    Schedule mapSpbstuScheduleByScheduleDay(List<Document> documents);
}
