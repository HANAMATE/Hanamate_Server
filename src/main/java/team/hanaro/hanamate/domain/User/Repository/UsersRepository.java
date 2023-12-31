package team.hanaro.hanamate.domain.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.hanaro.hanamate.entities.MyWallet;
import team.hanaro.hanamate.entities.User;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String id);
    boolean existsByLoginId(String id);
    Optional<User> findUserByPhoneNumber(String phoneNumber);
    Optional<User> findByMyWallet(MyWallet wallet);
}
