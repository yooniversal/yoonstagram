package yoonstagram.instagram.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Follow {

    @Id @GeneratedValue
    @Column(name = "follow_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "to_user_id")
    private User toUser;

    public Follow(User fromUser, User toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
}