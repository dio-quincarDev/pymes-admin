package auth.pymes.utils.pageable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse <T> {

    private List<T> content;

    private Long totalElements;

    private int totalPages;

    private int size;

    private int number;

}
