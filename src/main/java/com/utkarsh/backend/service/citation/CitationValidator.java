package com.utkarsh.backend.service.citation;

import com.utkarsh.backend.dto.CitationDto;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitationValidator {

    private static final Pattern CITATION_PATTERN =
            Pattern.compile("\\[C(\\d+)]");

    public List<CitationDto> validate(
            String answer,
            List<CitationDto> availableCitations) {

        Map<String, CitationDto> citationMap = new HashMap<>();

        for (CitationDto citation : availableCitations) {
            citationMap.put(citation.id(), citation);
        }

        Set<String> referencedIds = new LinkedHashSet<>();

        Matcher matcher = CITATION_PATTERN.matcher(answer);

        while (matcher.find()) {
            referencedIds.add("C" + matcher.group(1));
        }

        List<CitationDto> validCitations = new ArrayList<>();

        for (String id : referencedIds) {

            CitationDto citation = citationMap.get(id);

            if (citation != null) {
                validCitations.add(citation);
            }
        }

        return validCitations;
    }

}