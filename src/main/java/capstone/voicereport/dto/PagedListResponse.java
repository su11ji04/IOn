package capstone.voicereport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class PagedListResponse<T> {
    private List<T> content;
    private int totalPages;
    private int size;
}