package team.hanaro.hanamate.domain.Loan.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.hanaro.hanamate.entities.LoanHistory;
import team.hanaro.hanamate.entities.Loans;

import java.util.List;
import java.util.Optional;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {
    List<LoanHistory> findAllBySuccessIsTrueAndLoansLoanId(Long loanId);
    Optional<LoanHistory> findByLoansAndSuccessIsFalseOrderByHistoryId(Optional<Loans> loans);

}
