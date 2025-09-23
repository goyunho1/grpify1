package grpify.grpify.user.domain;

import grpify.grpify.PostLike.domain.PostLike;
import grpify.grpify.commentLike.domain.CommentLike;
import grpify.grpify.common.domain.BaseTimeEntity;
import grpify.grpify.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Setter
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100) // OAuth에서 받을 것이므로 NOT NULL & UNIQUE
    private String email;

    @Column(nullable = false, length = 255)
    @Builder.Default
    private String profileImgUrl = "/img/t3";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<CommentLike> commentLikes = new ArrayList<>();

    //연관관계 필드 (양방향 필요시 추가)
    //


    // 사용자 정보 업데이트 (프로필 이미지, 닉네임 등)
    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImgUrl = profileImageUrl;
    }

    // 사용자 권한 변경
    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public void dearetivate() {
        this.isDeleted = true;
    }
}
