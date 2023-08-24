package team.hanaro.hanamate.domain.Loan.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.hanaro.hanamate.entities.Child;
import team.hanaro.hanamate.entities.LoanHistory;
import team.hanaro.hanamate.entities.Loans;
import team.hanaro.hanamate.entities.Parent;

import java.util.List;
import java.util.Optional;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {
    Optional<List<LoanHistory>> findAllByLoansAndSuccessIsTrue(Optional<Loans> loans);

}