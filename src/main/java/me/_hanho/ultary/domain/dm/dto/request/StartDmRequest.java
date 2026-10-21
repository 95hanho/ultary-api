package me._hanho.ultary.domain.dm.dto.request;

import lombok.Getter;
import lombok.Setter;

/** 첫 메시지와 방 생성을 같은 트랜잭션으로 처리한다. */
@Getter
@Setter
public class StartDmRequest extends SendDmMessageRequest {
    private Long targetUserNo;
}
