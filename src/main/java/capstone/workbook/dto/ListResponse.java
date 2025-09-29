package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ListResponse {
    private String userId;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private List<ListItemDto> items;
}