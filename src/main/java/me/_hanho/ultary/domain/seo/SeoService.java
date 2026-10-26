package me._hanho.ultary.domain.seo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
@Service @RequiredArgsConstructor
public class SeoService {
 private final SeoMapper mapper;
 public record Page(List<SeoEntry> items, Long nextCursorId) {}
 @Transactional(readOnly=true)
 public Page list(String type, Long cursorId, int limit) {
  if (cursorId == null || cursorId < 0 || limit < 1 || limit > 1000
      || !("PROFILE".equals(type) || "FEED".equals(type))) throw new BusinessException(ErrorCode.INVALID_INPUT);
  var rows = "PROFILE".equals(type) ? mapper.profiles(cursorId,limit+1) : mapper.feeds(cursorId,limit+1);
  boolean more=rows.size()>limit;
  var items=more ? rows.subList(0,limit) : rows;
  return new Page(items,more ? items.get(items.size()-1).getId() : null);
 }
}
