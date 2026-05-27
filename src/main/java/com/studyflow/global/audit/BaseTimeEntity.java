package com.studyflow.global.audit;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false) // 생성일은 수정 불가능하게 설정
    private LocalDateTime createdAt; // insert 시 현재 시간이 자동으로 들어감

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // update 시 수정 시간이 자동으로 반영됨
}
