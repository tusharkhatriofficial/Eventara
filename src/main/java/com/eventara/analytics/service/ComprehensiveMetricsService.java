package com.eventara.analytics.service;

import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.common.dto.EventDto;
import com.eventara.common.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ComprehensiveMetricsService {

}
