package me._hanho.ultary.domain.admin;

import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.*;
import me._hanho.ultary.domain.admin.dto.response.*;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.pet.PetService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class AdminManagementService {
    private final AdminManagementMapper mapper;
    private final FileService files;
    private final PetService pets;
    private final me._hanho.ultary.domain.pet.PetMapper petMapper;

    public AdminPageResponse<AdminUserResponse> users(String q,String status,int page,int size) {
        validatePage(page,size); q=query(q);
        status=option(status,Set.of("ACTIVE","SUSPENDED","WITHDRAWN"));
        var rows=mapper.users(q,status,page*size,size);
        var summaries=files.findSummaries(rows.stream().map(AdminUserResponse::getProfileFileId).filter(java.util.Objects::nonNull).toList());
        rows.forEach(row->row.setProfileFile(summaries.get(row.getProfileFileId())));
        if(!rows.isEmpty()) {
            var names=new java.util.HashMap<Long,java.util.List<String>>();
            petMapper.findActiveMentions(rows.stream().map(AdminUserResponse::getUserNo).toList())
                .forEach(pet->names.computeIfAbsent(pet.getUserNo(),key->new java.util.ArrayList<>()).add(pet.getName()));
            rows.forEach(row->row.setPetNames(names.getOrDefault(row.getUserNo(),java.util.List.of())));
        }
        return new AdminPageResponse<>(rows,mapper.countUsers(q,status),page,size);
    }

    public AdminUserDetailResponse user(Long id) {
        if(id==null||id<=0)throw new BusinessException(ErrorCode.INVALID_INPUT);
        var row=mapper.user(id);
        if(row==null)throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        row.setProfileFile(files.findSummary(row.getProfileFileId()));
        var userPets=pets.listByOwner(id);
        row.setPetNames(userPets.stream().map(pet->pet.getName()).toList());
        return new AdminUserDetailResponse(row,userPets,mapper.feeds(id),mapper.reports(id));
    }

    public void assertProfileFile(Long userNo,Long fileId) {
        if(mapper.user(userNo)==null)throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if(pets.listByOwner(userNo).stream().noneMatch(p->fileId.equals(p.getProfileFileId())))
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }

    public AdminPageResponse<AdminActionLogResponse> actions(String q,String kind,int page,int size) {
        validatePage(page,size); q=query(q);kind=option(kind,Set.of("USER","TAG","REPORT","AI_REVIEW"));
        return new AdminPageResponse<>(mapper.actions(q,kind,page*size,size),mapper.countActions(q,kind),page,size);
    }

    /** 조치 실패 또는 이력 저장 실패 시 상태 변경도 함께 롤백한다. */
    @Transactional
    public <T> T execute(Long adminNo,String action,String kind,Long id,Long requestId,String reason,Supplier<T> operation) {
        if(adminNo==null)throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if(reason!=null&&reason.length()>500)throw new BusinessException(ErrorCode.INVALID_INPUT);
        var before=mapper.target(kind,id,requestId);
        T result=operation.get();
        var after=mapper.target(kind,id,requestId);
        var entry=new AdminActionLogResponse();
        entry.setAdminNo(adminNo);entry.setAction(action);entry.setTargetType(kind);entry.setTargetId(id);entry.setRequestId(requestId);
        entry.setTargetLabel(after!=null?after.getTargetLabel():before!=null?before.getTargetLabel():null);
        entry.setBeforeStatus(before==null?null:before.getAfterStatus());entry.setAfterStatus(after==null?null:after.getAfterStatus());
        entry.setReason(reason==null||reason.isBlank()?null:reason.trim());
        if(mapper.insertAction(entry)!=1)throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return result;
    }

    private void validatePage(int page,int size) {
        if(page<0||page>1000000||size<1||size>50)throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    private String query(String q) {
        if(q==null||q.isBlank())return null;
        q=q.trim();if(q.startsWith("@"))q=q.substring(1);
        if(q.length()>100)throw new BusinessException(ErrorCode.INVALID_INPUT);
        return q.isBlank()?null:q;
    }
    private String option(String value,Set<String> allowed) {
        if(value==null||value.isBlank()||value.equals("ALL"))return null;
        if(!allowed.contains(value))throw new BusinessException(ErrorCode.INVALID_INPUT);
        return value;
    }
}
