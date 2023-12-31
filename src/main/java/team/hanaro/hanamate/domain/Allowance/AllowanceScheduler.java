package team.hanaro.hanamate.domain.Allowance;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import team.hanaro.hanamate.domain.Loan.Repository.LoanHistoryRepository;
import team.hanaro.hanamate.domain.Loan.Repository.LoanRepository;
import team.hanaro.hanamate.domain.MyWallet.Repository.MyWalletRepository;
import team.hanaro.hanamate.domain.MyWallet.Repository.TransactionRepository;
import team.hanaro.hanamate.domain.User.Repository.ChildRepository;
import team.hanaro.hanamate.domain.User.Repository.ParentRepository;
import team.hanaro.hanamate.entities.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
@Component
public class AllowanceScheduler {

    private final AllowancesRepository allowancesRepository;
    private final TransactionRepository transactionRepository;
    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final MyWalletRepository myWalletRepository;
    private final LoanRepository loanRepository;
    private final LoanHistoryRepository loanHistoryRepository;

    //@Scheduled(fixedDelay = 1000) 1초 단위 스케줄러
    public void scheduleFixedDelayTask() throws InterruptedException {
        System.out.println("Scheduler Check - {" + System.currentTimeMillis() / 1000 + "}");
        Thread.sleep(5000);
    }

    //@Scheduled(fixedDelay = 1000 * 60) //테스트를 위해서 추가
    @Scheduled(cron = "0 0 6 * * *") //매일 오전 6시에 실행하는 cron
    @Transactional
    public void scheduleTaskUsingCronExpression() {
        System.out.println("Scheduler tasks using cron jobs - {" + System.currentTimeMillis() / 1000 + "}");

        // 해야할 일
        // 1. 오늘 날짜 가져오기
        LocalDateTime localDateTime = LocalDateTime.now();
        int currentDayOfWeek = localDateTime.getDayOfWeek().getValue(); /* 월요일(1) ~ 일요일(7) */
        int currentDate = localDateTime.getDayOfMonth();
        //System.out.println("currentDate: "+currentDate+", currentDayOfWeek: "+currentDayOfWeek);
        // 2. 오늘 날짜와 연관된 정기용돈 목록 불러오기
        //List<Allowances> allowances = allowancesRepository.findAllByEverydayOrDayOfWeekOrTransferDateAndValidIsTrue(true, currentDayOfWeek, currentDate);
        List<Allowances> allowances = allowancesRepository.findAllByTransferDateAndValidIsTrue(currentDate);

        if (!allowances.isEmpty()) {
            // 3. 용돈 이체 일으키기
            for (Allowances allowance : allowances) {
                System.out.println(allowance.toString());

                // 3-1. allowance의 parentIdx와 childrenIdx로 User 불러오기
                Optional<Child> child = childRepository.findById(allowance.getChildrenIdx());
                Optional<Parent> parent = parentRepository.findById(allowance.getParentIdx());
                Optional<MyWallet> cWallet = myWalletRepository.findById(allowance.getChildrenIdx());
                Optional<MyWallet> pWallet = myWalletRepository.findById(allowance.getParentIdx());
                if (child.isEmpty() || parent.isEmpty() || cWallet.isEmpty() || pWallet.isEmpty()) {
                    break;
                }
                MyWallet childWallet = cWallet.get();
                MyWallet parentWallet = pWallet.get();

                int monthly_allowance_amount = allowance.getAllowanceAmount();

                //TODO: 3-2. allowanceAmount>parent지갑 잔액이 작으면 실패 --> 거래 실패 이력 남기기
                if (parentWallet.getBalance() < allowance.getAllowanceAmount()) {
                    System.out.println("실패");
                    break;
                }

                //대출 있는지 확인
                Optional<Loans> loans = loanRepository.findByChildAndCompletedIsFalse(child.get());
                if (loans.isPresent()) {
                    Optional<LoanHistory> history = loanHistoryRepository.findByLoansAndSuccessIsFalseOrderByHistoryId(loans);

                    if (history.isPresent()) {
                        //대출만큼 금액에서 까기
                        int monthly_repayment_amount = history.get().getRepaymentAmount();
                        monthly_allowance_amount -= monthly_repayment_amount;
                        if (monthly_allowance_amount < 0) {
                            monthly_allowance_amount = 0;
                        }
                    }
                    history.get().setSuccess(true);
                    loanHistoryRepository.save(history.get());

                    Optional<LoanHistory> result = loanHistoryRepository.findByLoansAndSuccessIsFalseOrderByHistoryId(loans);
                    //더이상 상환해야하는 loanHistory가 없다면
                    if (result.isEmpty()) {
                        loans.get().setCompleted(true);
                        loanRepository.save(loans.get());
                    }
                }

                // 3-3. 아이 지갑 +
                childWallet.setBalance(childWallet.getBalance() + monthly_allowance_amount);
                myWalletRepository.save(childWallet);
                // 3-4. 부모 지갑 -
                parentWallet.setBalance(parentWallet.getBalance() - monthly_allowance_amount);
                myWalletRepository.save(parentWallet);
                // 3-5. Transaction 작성
                makeTransaction(parent.get(), child.get(), monthly_allowance_amount);
                //sequence 성공 이력 남기기
            }
        }
    }

    public void makeTransaction(User parent, User child, Integer amount) {
        // 부모 -> 아이 거래내역
        Transactions parentToChildTransaction = Transactions.builder()
                .wallet(parent.getMyWallet())
                .counterId(child.getIdx())
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType("정기 용돈 이체")
                .amount(amount)
                .balance(parent.getMyWallet().getBalance())
                .build();
        // 아이 -> 부모 거래내역
        Transactions childToParentTransaction = Transactions.builder()
                .wallet(child.getMyWallet())
                .counterId(parent.getIdx())
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType("정기 용돈 입금")
                .amount(amount)
                .balance(child.getMyWallet().getBalance())
                .build();

        transactionRepository.save(parentToChildTransaction);
        transactionRepository.save(childToParentTransaction);
        transactionRepository.flush();
    }

}
