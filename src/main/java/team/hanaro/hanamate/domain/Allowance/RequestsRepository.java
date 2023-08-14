package team.hanaro.hanamate.domain.Allowance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import team.hanaro.hanamate.entities.Requests;

import java.util.List;
import java.util.Optional;

@Transactional
public interface RequestsRepository extends JpaRepository<Requests, Long> {
    Optional<List<Requests>> findAllByRequesterIdxAndAskAllowanceIsNull(Long aLong);

    Optional<List<Requests>> findByRequesterIdx(Long aLong);

    Optional<List<Requests>> findAllByRequesterIdxAndAskAllowanceIsNotNull(Long aLong);

    Optional<List<Requests>> findAllByTargetIdxAndAskAllowanceIsNull(Long aLong);

    Optional<List<Requests>> findAllByTargetIdxAndAskAllowanceIsNotNull(Long aLong);

    Optional<List<Requests>> findTop20ByRequesterIdxAndAskAllowanceIsNullOrderByCreateDateDesc(Long aLong);

    Optional<List<Requests>> findTop20ByRequesterIdxAndAskAllowanceIsNotNullOrderByModifiedDateDesc(Long aLong);

    Optional<List<Requests>> findTop20ByTargetIdxAndAskAllowanceIsNullOrderByCreateDateDesc(Long aLong);

    Optional<List<Requests>> findTop20ByTargetIdxAndAskAllowanceIsNotNullOrderByModifiedDateDesc(Long aLong);

    Optional<Requests> findByRequestId(Long aLong);

    Long countAllByRequesterIdxAndAskAllowanceIsNull(Long aLong);
}
