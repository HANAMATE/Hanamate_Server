package team.hanaro.hanamate.entities;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long userId;

    private Long walletId; /* 개인 지갑 Id */
    private Long accountId;

    private String userName;
    private String loginId;

    private String loginPw;

    private String salt;

    private String identification;

    private String phoneNumber;

    private Integer age;
    private Timestamp registrationDate;

    private boolean userType; /* 0: 부모, 1: 아이 */

}
