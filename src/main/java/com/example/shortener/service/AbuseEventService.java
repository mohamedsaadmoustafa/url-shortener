package com.example.shortener.service;

import com.example.shortener.entity.AbuseEvent;
import com.example.shortener.repository.AbuseEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AbuseEventService {

    private final AbuseEventRepository abuseEventRepository;

    /**
     * Record an abuse event for a URL or user.
     */
    public void recordEvent(AbuseEvent event) {
        abuseEventRepository.save(event);
    }

}
