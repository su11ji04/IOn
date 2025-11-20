package capstone.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PagedListResponse<T> {
    private List<T> content;
    private int totalPages;
    private int size;
}