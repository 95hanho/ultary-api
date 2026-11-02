package me._hanho.ultary.domain.feed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.validation.HashtagRules;
import me._hanho.ultary.domain.tag.TagMapper;
import me._hanho.ultary.domain.tag.model.Tag;

/** Read tags for inspection first; create missing tags only inside the approved feed write. */
@Service
@RequiredArgsConstructor
public class FeedCaptionTags {
    private static final Pattern TOKEN = Pattern.compile("(?<!\\S)#([0-9A-Za-z가-힣_]+)");
    private final TagMapper mapper;

    public String inspectionText(String content, List<Long> tagIds) {
        var bodies = bodies(content);
        StringBuilder text = new StringBuilder(content == null ? "" : content);
        if (tagIds != null) for (Long id : new LinkedHashSet<>(tagIds)) {
            if (id == null) continue;
            Tag tag = mapper.findActiveByTagId(id);
            if (tag == null) throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
            String body = tag.getHashtag();
            if (bodies.putIfAbsent(body.toLowerCase(Locale.ROOT), body) == null) {
                if (!text.isEmpty()) text.append('\n');
                text.append('#').append(body);
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> resolve(Long userNo, String content, List<Long> tagIds) {
        return resolve(userNo, content, tagIds, List.of());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> resolve(Long userNo, String content, List<Long> tagIds, List<Long> retainedIds) {
        var ids = new LinkedHashSet<Long>();
        if (tagIds != null) tagIds.stream().filter(java.util.Objects::nonNull).forEach(ids::add);
        var preferred = new LinkedHashMap<String, Tag>();
        var candidates = new LinkedHashSet<>(ids);
        candidates.addAll(retainedIds);
        for (Long id : candidates) {
            Tag tag = mapper.findActiveByTagId(id);
            if (tag != null) preferred.putIfAbsent(tag.getHashtag().toLowerCase(Locale.ROOT), tag);
        }
        for (String body : bodies(content).values()) {
            Tag tag = preferred.get(body.toLowerCase(Locale.ROOT));
            if (tag == null) tag = mapper.findActiveByHashtag(body);
            if (tag == null) {
                tag = new Tag();
                tag.setHashtag(body);
                tag.setPurpose("USER");
                tag.setProfileStatus("NONE");
                tag.setCreatedByUserNo(userNo);
                mapper.insert(tag);
            }
            ids.add(tag.getTagId());
        }
        if (ids.size() > 30) throw new BusinessException(ErrorCode.INVALID_INPUT, "태그는 최대 30개입니다.");
        return new ArrayList<>(ids);
    }

    private LinkedHashMap<String, String> bodies(String content) {
        var result = new LinkedHashMap<String, String>();
        var matcher = TOKEN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            String body = matcher.group(1);
            if (!body.matches(HashtagRules.REGEX)) throw new BusinessException(ErrorCode.INVALID_INPUT, HashtagRules.MESSAGE);
            result.putIfAbsent(body.toLowerCase(Locale.ROOT), body);
        }
        return result;
    }
}
