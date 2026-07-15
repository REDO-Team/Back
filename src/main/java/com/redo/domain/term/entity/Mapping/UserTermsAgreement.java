package com.redo.domain.term.entity.Mapping;

import com.redo.domain.term.entity.Term;
import com.redo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_terms_agreements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "term_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "agreed", nullable = false)
    private Boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    public static UserTermsAgreement create(User user, Term term) {
        UserTermsAgreement agreement = new UserTermsAgreement();
        agreement.user = user;
        agreement.term = term;
        agreement.agreed = true;
        agreement.agreedAt = LocalDateTime.now();
        return agreement;
    }
}