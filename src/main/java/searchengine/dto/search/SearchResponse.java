package searchengine.dto.search;

import lombok.Data;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<DetailedSearchItem> data;
    private ResponseEntity<?> entity;
}
