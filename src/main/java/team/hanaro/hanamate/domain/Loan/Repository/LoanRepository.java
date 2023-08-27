package team.hanaro.hanamate.domain.Loan.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import team.hanaro.hanamate.entities.*;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loans, Long> {

    Optional<Loans> findByLoanId(String user);
//    List<Loans> findByLoanId(Loans id);
    List<Loans> findByChild(Child id);
    List<Loans> findByParent(Parent id);

    void deleteById(Long loanId);

    Optional<List<Loans>> findAllByChild(Child nowChild);
    Optional<List<Loans>> findAllByChildAndValidIsTrueAndCompletedIsTrue(Child child);
    Optional<List<Loans>> findAllByParentAndValidIsTrueAndCompletedIsTrue(Parent parent);

//    Optional<List<LoanHistory>> findAllByChildAndSuccessIsTrue(Child nowChild);
}
