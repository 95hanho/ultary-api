package me._hanho.ultary.domain.feed;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.feed.dto.request.CommentMentionRequest;
import me._hanho.ultary.domain.feed.model.FeedCommentMention;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.common.exception.*;

@Service
@RequiredArgsConstructor
public class CommentMentionService {
    private final FeedMapper mapper;
    private final UserMapper users;
    private final PetMapper pets;
    private final PrivacyService privacy;
    private final ObjectMapper json;
    private static final Pattern TOKEN = Pattern.compile("(?<!\\S)@([\\p{L}\\p{N}_]+)");

    public void replace(Long actor, Long commentId, Long replyId, String content, List<CommentMentionRequest> supplied) {
        replaceInternal(actor,commentId,replyId,content,supplied,true);
        capture(commentId,replyId,content);
    }
    public void capture(Long commentId, Long replyId, String content) {
        var links = commentId != null ? mapper.findMentionsByCommentId(commentId) : mapper.findMentionsByReplyId(replyId);
        var targets = links.stream().map(link -> {
            Map<String,Long> item = new LinkedHashMap<>();
            if (link.getMentionedUserNo()!=null) item.put("userNo",link.getMentionedUserNo());
            if (link.getMentionedPetId()!=null) item.put("petId",link.getMentionedPetId());
            return item;
        }).toList();
        try { mapper.saveMentionSnapshot(commentId!=null?"COMMENT":"REPLY",commentId!=null?commentId:replyId,content,json.writeValueAsString(targets)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Cannot save mention identities",e); }
    }
    /** Restore only identities explicitly selected for this body, never infer from plain @text. */
    public void restore(Long actor, Long commentId, Long replyId, String content) {
        String snapshot=mapper.findMentionSnapshot(commentId!=null?"COMMENT":"REPLY",commentId!=null?commentId:replyId,content);
        try {
            List<CommentMentionRequest> targets=snapshot==null?List.of():json.readValue(snapshot,new TypeReference<List<CommentMentionRequest>>(){});
            replaceInternal(actor,commentId,replyId,content,targets,false);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Cannot restore mention identities",e); }
    }
    private void replaceInternal(Long actor, Long commentId, Long replyId, String content, List<CommentMentionRequest> supplied, boolean validate) {
        Set<String> labels=new HashSet<>();var matches=TOKEN.matcher(content==null?"":content);
        while(matches.find()) labels.add(matches.group(1));
        Map<String,FeedCommentMention> wanted=new LinkedHashMap<>();
        if(supplied!=null) for(var item:supplied) {
            if(item==null||!item.isValidTarget())throw new BusinessException(ErrorCode.INVALID_INPUT);
            var mention=new FeedCommentMention();
            if(item.getUserNo()!=null) {
                var user=users.findActiveByUserNo(item.getUserNo());
                if(user==null) {if(!validate)continue;throw new BusinessException(ErrorCode.USER_NOT_FOUND);}
                if(validate&&!labels.contains(user.getNickname()))continue;
                if(validate)privacy.assertMentionAllowed(actor,user.getUserNo());
                mention.setMentionedUserNo(user.getUserNo());
            } else {
                var pet=pets.findActiveByPetId(item.getPetId());
                if(pet==null)continue;
                if(validate&&!labels.contains(pet.getMentionId()))continue;
                if(validate)privacy.assertMentionAllowed(actor,pet.getUserNo());
                mention.setMentionedPetId(pet.getPetId());
            }
            wanted.put(key(mention),mention);
        }
        if(wanted.size()>20)throw new BusinessException(ErrorCode.INVALID_INPUT,"멘션은 최대 20개입니다.");
        var existing=commentId!=null?mapper.findMentionsByCommentId(commentId):mapper.findMentionsByReplyId(replyId);
        for(var old:existing)if(wanted.remove(key(old))==null)mapper.deleteCommentMention(old.getFeedCommentMentionId());
        for(var mention:wanted.values()){mention.setFeedCommentId(commentId);mention.setFeedReplyId(replyId);mapper.insertCommentMention(mention);}
    }
    private static String key(FeedCommentMention item){return item.getMentionedUserNo()!=null?"U:"+item.getMentionedUserNo():"P:"+item.getMentionedPetId();}
}
