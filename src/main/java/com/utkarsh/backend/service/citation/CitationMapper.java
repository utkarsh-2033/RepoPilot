package com.utkarsh.backend.service.citation;

import com.utkarsh.backend.dto.CitationDto;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CitationMapper {

    public List<CitationDto> fromDocuments(List<Document> documents) {

        List<CitationDto> citations = new ArrayList<>();

        for (int i = 0; i < documents.size(); i++) {

            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();
            citations.add(new CitationDto(
                    "C" + (i + 1),
                    stringValue(metadata.get("filePath")),
                    intValue(metadata.get("startLine")),
                    intValue(metadata.get("endLine")),
                    stringValue(metadata.get("language"))
            ));
        }

        return citations;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value) {

        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) return null;

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}