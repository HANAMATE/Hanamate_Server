package team.hanaro.hanamate.domain.Loan.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import team.hanaro.hanamate.domain.Allowance.AllowanceService;
import team.hanaro.hanamate.domain.Loan.Dto.LoanRequestDto;
import team.hanaro.hanamate.domain.Loan.Dto.LoanResponseDto;
import team.hanaro.hanamate.domain.Loan.Repository.LoanHistoryRepository;
import team.hanaro.hanamate.domain.Loan.Repository.LoanRepository;
import team.hanaro.hanamate.domain.User.Repository.ChildRepository;
import team.hanaro.hanamate.domain.User.Repository.ParentRepository;
import team.hanaro.hanamate.domain.User.Repository.UsersRepository;
import team.hanaro.hanamate.entities.*;
import team.hanaro.hanamate.global.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final Response response;
    private final UsersRepository usersRepository;
    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final AllowanceService allowanceService;

    private final LoanHistoryRepository loanHistoryRepository;

    public ResponseEntity<?> initLoanInfo() {
        LoanResponseDto.initInfo initInfo = new LoanResponseDto.initInfo();
        initInfo.setInterestRate(1);
        initInfo.setPaymentMethod("원금균등상환");
        return response.success(initInfo, "정상적으로 대출 초기 정보를 가져왔습니다.", HttpStatus.OK);
    }

    public ResponseEntity<?> apply(LoanRequestDto.Apply apply, String userId) {

        Optional<Child> maybeUser = childRepository.findByLoginId(userId);
        if (maybeUser.isEmpty()) {
            return response.fail("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        Child now_user = maybeUser.get();
        Loans loans = Loans.builder()
                .child(now_user)
                .parent(now_user.getMyParentList().get(0).getParent())
                .walletId(now_user.getMyWallet().getId())
                .loanName(apply.getLoanName())
                .loanAmount(Integer.valueOf(apply.getLoanAmount()))
                .loanMessage(apply.getLoanMessage())
                .interestRate(1)
                .paymentMethod("원금균등상환")
                .completed(false)
                .valid(false)
                .total_interestRate(apply.getTotal_interestRate())
                .total_repaymentAmount(apply.getTotal_repaymentAmount())
                .balance(apply.getBalance())
                .sequence(apply.getSequence())
                .build();
        loanRepository.save(loans);

        return response.success("대출 신청이 완료되었습니다.");
    }

    public ResponseEntity<?> calculate(LoanRequestDto.Calculate calculate, String userId) {
        Optional<Child> maybeChild = childRepository.findByLoginId(userId);
        if (maybeChild.isEmpty()) {
            return response.fail("해당하는 사용자 정보가 없습니다.", HttpStatus.BAD_REQUEST);
        }
        Child now_user = maybeChild.get();
        Integer allowance = allowanceService.getPeriodicAllowanceByChildId(now_user);// ByChildID라는 함수를 가져왔다는 가정으로

        if (allowance == null) {
            return response.fail("정기용돈이 존재하지 않아 대출 신청을 할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        ArrayList<Integer> loanAmountList = new ArrayList<>();
        ArrayList<Integer> repaymentList = new ArrayList<>();
        Integer interestRate = calculate.getInterestRate();
        Integer sequence = calculate.getSequence();
        Integer loanAmount = calculate.getLoanAmount();
        Integer balance = loanAmount;

        Integer total_interestRate = 0;
        Integer interestRates = 0;
        Integer month_amount = loanAmount / sequence;
        for (int i = 0; i < sequence; i++) {
            interestRates = (int) Math.round((interestRate * 0.01) / 12 * balance);
            loanAmountList.add(interestRates); //(이자값이 회차마다 달라지기 때문에 각 배열에 넣음)
            total_interestRate += interestRates; //총이자 계산
            balance -= month_amount; //원금에서의 잔액 계산
            repaymentList.add(interestRates + month_amount);
        }
        Integer total_loanAmount = loanAmount + total_interestRate; //총납입금액
        //빌릴 수 있는 최대 금액 계산
        Integer maxLoanAmount = Collections.max(repaymentList);
        if (allowance < maxLoanAmount) {
            ResponseEntity<?> responseEntity = response.fail("대출 한도가 넘어 대출 신청을 할 수 없습니다.", HttpStatus.BAD_REQUEST);
            return responseEntity;
        }

        LoanResponseDto.CalculateResult calculateResult = new LoanResponseDto.CalculateResult();
        calculateResult.setLoanAmountList(loanAmountList);
        calculateResult.setRepaymentList(repaymentList);
        calculateResult.setTotal_interestRate(total_interestRate);
        calculateResult.setTotal_repaymentAmount(total_loanAmount);

        return response.success(calculateResult, "정상적으로 대출 맞춤 정보를 계산하였습니다.", HttpStatus.OK);


    }

    //부모 - 아이 화면에서 대출 신청 정보 가져오기 (대출에 관련된 부모, 아이만 해당 정보를 가져올 수 있음 아니면 에러남)
    public ResponseEntity<?> applyInfo(String userId) {
        Optional<User> maybeUser = usersRepository.findByLoginId(userId);
        if (maybeUser.isEmpty()) {
            return response.fail("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        User now_user = maybeUser.get();
        LoanResponseDto.applyInfo applyInfo = new LoanResponseDto.applyInfo();

        if (now_user.getUserType().equals("Child")) {
            Optional<Child> maybeChild = childRepository.findByLoginId(userId);

            if (maybeChild.isEmpty()) {
                return response.fail("잘못된 접근입니다.", HttpStatus.BAD_REQUEST);
            }

            Child now_child = maybeChild.get();
            List<Loans> loansList = loanRepository.findByChild(now_child);

            // Child의 대출 목록 중에서 유효한 대출만 필터링
            // 유효한 대출 중에서 첫 번째 대출 정보를 가져오기
            Optional<Loans> validLoan = loansList.stream()
                    .filter(loan -> !loan.getCompleted()) // getCompleted가 false인 대출만 선택
                    .findFirst();

            if (validLoan.isEmpty()) {
                LoanResponseDto.applyNotInfo applyNotInfo = new LoanResponseDto.applyNotInfo();
                applyNotInfo.setUserType(now_user.getUserType());
                return response.fail(applyNotInfo, "신청한 대출 상품이 없습니다.", HttpStatus.NO_CONTENT);
            }
            Loans nowLoan = validLoan.get();

            applyInfo.setUserType(now_user.getUserType());
            applyInfo.setLoanName(nowLoan.getLoanName());
            applyInfo.setLoanAmount(nowLoan.getLoanAmount());
            applyInfo.setLoanMessage(nowLoan.getLoanMessage());
            applyInfo.setSequence(nowLoan.getSequence());
            applyInfo.setValid(nowLoan.getValid());

        } else {
            Optional<Parent> maybeParent = parentRepository.findByLoginId(userId);

            if (maybeParent.isEmpty()) {
                return response.fail("잘못된 접근입니다.", HttpStatus.BAD_REQUEST);
            }

            Parent now_parent = maybeParent.get();
            List<Loans> loansList = loanRepository.findByParent(now_parent);

            Optional<Loans> validLoan = loansList.stream()
                    .filter(loan -> !loan.getCompleted()) // getCompleted가 false인 대출만 선택
                    .findFirst();

            if (validLoan.isEmpty()) {
                LoanResponseDto.applyNotInfo applyNotInfo = new LoanResponseDto.applyNotInfo();
                applyNotInfo.setUserType(now_user.getUserType());
                return response.fail(applyNotInfo, "신청한 대출 상품이 없습니다.", HttpStatus.NO_CONTENT);
            }
            Loans nowLoan = validLoan.get();

            applyInfo.setUserType(now_user.getUserType());
            applyInfo.setLoanName(nowLoan.getLoanName());
            applyInfo.setLoanAmount(nowLoan.getLoanAmount());
            applyInfo.setLoanMessage(nowLoan.getLoanMessage());
            applyInfo.setSequence(nowLoan.getSequence());
            applyInfo.setValid(nowLoan.getValid());
        }

        return response.success(applyInfo, "정상적으로 대출 신청 정보를 가져왔습니다.", HttpStatus.OK);

    }

    //history 정보에 값 넣기
    public ResponseEntity<?> approve(LoanRequestDto.Approve approve, String userId) {
        Parent now_parent = parentRepository.findByLoginId(userId).get();
        List<Loans> loansList = loanRepository.findByParent(now_parent);

        Optional<Loans> validLoan = loansList.stream()
                .filter(loan -> !loan.getCompleted()) // getCompleted가 false인 대출만 선택
                .findFirst();

        if (validLoan.isEmpty()) {
            LoanResponseDto.applyNotInfo applyNotInfo = new LoanResponseDto.applyNotInfo();
            applyNotInfo.setUserType(now_parent.getUserType());
            return response.fail(applyNotInfo, "신청한 대출 상품이 없습니다.", HttpStatus.NO_CONTENT);
        }

            Loans existingLoan = validLoan.get();

            existingLoan.setValid(true);
            existingLoan.setStartDate(approve.getStartDate());
            existingLoan.setEndDate(approve.getEndDate());
            existingLoan.setDuration(approve.getDuration());

            loanRepository.save(existingLoan); // 새로운 객체로 업데이트


            //이때 history값을 다 넣어주고 용돈 안내면 false로 하고, 내면 true로 하고, 프론트에서 보여줄때는 true인것만 보여주는 식으로.. 하면 될거 같은데
            //뿐만 아니라 잔액에서 깎이도록 해야함!
            Integer interestRate = existingLoan.getInterestRate();
            Integer sequence = existingLoan.getSequence();
            Integer loanAmount = existingLoan.getLoanAmount();
            Integer balance = loanAmount;

            Integer total_interestRate = 0;
            Integer interestRates = 0;
            Integer month_amount = loanAmount / sequence;

            for (int i = 0; i < sequence; i++) {
                interestRates = (int) Math.round((interestRate * 0.01) / 12 * balance);
                total_interestRate += interestRates; //총이자 계산
                balance -= month_amount; //원금에서의 잔액 계산

                LoanHistory loanHistory = LoanHistory.builder()
                        .sequence_time(i + 1)
                        .transactionDate(approve.getStartDate()) //TODO: 월 단위로 바뀌도록 설정해야함.일단 임시로 시작날짜로 해놓음
                        .repaymentAmount(interestRates + month_amount)
                        .success(false)
                        .loans(existingLoan)
                        .build();
                loanHistoryRepository.save(loanHistory);
            }
        return response.success(null, "정상적으로 대출을 승인했습니다.", HttpStatus.OK);
    }

    public ResponseEntity<?> refuse(String userId) {

        Optional<Parent> maybeParent = parentRepository.findByLoginId(userId);

        if (maybeParent.isEmpty()){
            return response.fail("잘못된 접근입니다.", HttpStatus.BAD_REQUEST);
        }
        Parent now_parent = maybeParent.get();
        List<Loans> loansList = loanRepository.findByParent(now_parent);

        Optional<Loans> validLoan = loansList.stream()
                .filter(loan -> !loan.getCompleted()) // getCompleted가 false인 대출만 선택
                .findFirst();

        if (validLoan.isEmpty()) {
            LoanResponseDto.applyNotInfo applyNotInfo = new LoanResponseDto.applyNotInfo();
            applyNotInfo.setUserType(now_parent.getUserType());
            return response.fail(applyNotInfo, "신청한 대출 상품이 없습니다.", HttpStatus.NO_CONTENT);
        }

        Long now_loanId = validLoan.get().getLoanId();
        loanRepository.deleteById(now_loanId);

        return response.success(null, "정상적으로 대출이 거절되어 요청이 삭제됐습니다.", HttpStatus.OK);
    }

    public ResponseEntity<?> historyInfo(String userId) {
        User now_user = usersRepository.findByLoginId(userId).get();

        if (now_user.getUserType().equals("Child")) {
            Child now_child = childRepository.findByLoginId(userId).get();
            Optional<List<Loans>> optionalLoans = loanRepository.findAllByChildAndValidIsTrueAndCompletedIsTrue(now_child);

            if (optionalLoans.isPresent()) {
                List<Loans> loanHistories = optionalLoans.get();
                List<LoanResponseDto.historyInfo> historyInfoList = new ArrayList<>();
                for (Loans loanHistory : loanHistories) {
                    LoanResponseDto.historyInfo historyInfoDto = new LoanResponseDto.historyInfo(loanHistory);
                    historyInfoList.add(historyInfoDto);
                }
                return response.success(historyInfoList, "나의 대출 내역 조회에 성공했습니다", HttpStatus.OK);
            } else {
                return response.fail("나의 대출 내역 조회에 실패했습니다.", HttpStatus.BAD_REQUEST);
            }
        } else {
            Parent now_parent = parentRepository.findByLoginId(userId).get();
            Optional<List<Loans>> optionalLoans = loanRepository.findAllByParentAndValidIsTrueAndCompletedIsTrue(now_parent);

            if (optionalLoans.isPresent()) {
                List<Loans> loanHistories = optionalLoans.get();
                List<LoanResponseDto.historyInfo> historyInfoList = new ArrayList<>();
                for (Loans loanHistory : loanHistories) {
                    LoanResponseDto.historyInfo historyInfoDto = new LoanResponseDto.historyInfo(loanHistory);
                    historyInfoList.add(historyInfoDto);
                }
                return response.success(historyInfoList, "아이의 대출 내역 조회에 성공했습니다", HttpStatus.OK);
            } else {
                return response.fail("아이의 대출 내역 조회에 실패했습니다.", HttpStatus.BAD_REQUEST);
            }
        }
    }


    public ResponseEntity<?> historydetailInfo(@PathVariable Long loanId, String userId) {
        List<LoanHistory> loanHistories = loanHistoryRepository.findAllBySuccessIsTrueAndLoansLoanId(loanId);
        if (!loanHistories.isEmpty()) {
            List<LoanResponseDto.historydetailInfo> historydetailInfoList = new ArrayList<>();
            for (LoanHistory loanHistory : loanHistories) {
                LoanResponseDto.historydetailInfo historydetailInfo = new LoanResponseDto.historydetailInfo(loanHistory);
                historydetailInfoList.add(historydetailInfo);
            }
            return response.success(historydetailInfoList, "나의 대출 상세 내역 조회에 성공했습니다", HttpStatus.OK);
        } else {
            return response.fail("나의 대출 상세 내역 조회에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> loandetailInfo(Long loanId) {
        Optional<Loans> optionalLoan = loanRepository.findById(loanId);
        LoanResponseDto.loandetailInfo loandetailInfo = new LoanResponseDto.loandetailInfo();
        if (optionalLoan.isPresent()) {
            Loans loan = optionalLoan.get();
            loandetailInfo.setLoanName(loan.getLoanName());
            loandetailInfo.setLoanMessage(loan.getLoanMessage());
            loandetailInfo.setLoanAmount(loan.getLoanAmount());
            loandetailInfo.setTotal_interestRate(loan.getTotal_interestRate());
            loandetailInfo.setTotal_repaymentAmount(loan.getTotal_repaymentAmount());
            loandetailInfo.setInterestRate(loan.getInterestRate());
            loandetailInfo.setPaymentMethod(loan.getPaymentMethod());
            loandetailInfo.setSequence(loan.getSequence());
            loandetailInfo.setStartDate(loan.getStartDate());
            loandetailInfo.setEndDate(loan.getEndDate());
            return response.success(loandetailInfo, "대출 상세 정보 조회에 성공했습니다.", HttpStatus.OK);
        } else {
            return response.fail("대출 상세 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

    }
}
