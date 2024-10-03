package com.funtikov.sch_parser.service;

import com.funtikov.sch_parser.model.Schedule;
import com.funtikov.sch_parser.model.api.response.sseu.SseuApiResponse;

import java.util.List;

public interface MappingService {
    Schedule mapSseuToSchedule(List<SseuApiResponse> weekSseuSchedules);
}
