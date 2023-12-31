package team.hanaro.hanamate.domain.MyWallet;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import team.hanaro.hanamate.domain.MyWallet.Dto.RequestDto;
import team.hanaro.hanamate.domain.MyWallet.Dto.ResponseDto;
import team.hanaro.hanamate.domain.MyWallet.Repository.AccountRepository;
import team.hanaro.hanamate.domain.MyWallet.Repository.MyWalletRepository;
import team.hanaro.hanamate.domain.MyWallet.Repository.TransactionRepository;
import team.hanaro.hanamate.domain.User.Repository.UsersRepository;
import team.hanaro.hanamate.entities.*;
import team.hanaro.hanamate.global.Response;

import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final MyWalletRepository walletRepository;
    private final UsersRepository usersRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final Response response;

    public ResponseEntity<?> myWallet(String loginId) {
        Optional<User> userInfo = usersRepository.findByLoginId(loginId);

        if (userInfo.isEmpty()) {
            return response.fail("유저 Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        MyWallet wallet = userInfo.get().getMyWallet();
        ResponseDto.Wallet myWalletResDto = new ResponseDto.Wallet(wallet);

        return response.success(myWalletResDto, "내 지갑 잔액 조회에 성공했습니다.", HttpStatus.OK);

    }

    //TODO : Timestamp -> LocalDateTime 수정 필요
    public ResponseEntity<?> myWalletTransactions(String loginId) {
        Optional<User> userInfo = usersRepository.findByLoginId(loginId);

        if (userInfo.isEmpty()) {
            return response.fail("유저 Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        Integer year = Calendar.getInstance().get(Calendar.YEAR);
        Integer month = Calendar.getInstance().get(Calendar.MONTH) + 1;

        /*
        if (!user.getYear().equals(null) && !user.getMonth().equals(null)) {
            year = user.getYear();
            month = user.getMonth();
        } else {
            year = Calendar.getInstance().get(Calendar.YEAR);
            month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        }*/

        HashMap<String, Timestamp> map = getDate(year, month);

        List<Transactions> transactionsList = transactionRepository.findAllByWalletIdAndTransactionDateBetween(userInfo.get().getMyWallet().getId(), map.get("startDate"), map.get("endDate"));

        if (transactionsList.isEmpty()) {
            return response.fail("거래 내역이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<ResponseDto.MyTransactions> transactionResDtoList = new ArrayList<>();
        for (Transactions transaction : transactionsList) {
            ResponseDto.MyTransactions myWalletTransactionResDTO = new ResponseDto.MyTransactions(transaction);
            transactionResDtoList.add(myWalletTransactionResDTO);
        }
        return response.success(transactionResDtoList, "내 지갑 거래 내역 조회에 성공했습니다.", HttpStatus.OK);

    }

    public ResponseEntity<?> getAccount(String loginId) {
        Optional<User> userInfo = usersRepository.findByLoginId(loginId);

        if (userInfo.isEmpty()) {
            return response.fail("유저 Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<Account> account = accountRepository.findByUserId(userInfo.get().getIdx());

        if (account.isEmpty()) {
            return response.fail("연결된 계좌가 없습니다.", HttpStatus.BAD_REQUEST);
        }

        ResponseDto.AccountInfo accountResDto = new ResponseDto.AccountInfo(account.get());
        return response.success(accountResDto, "연결된 계좌 잔액 조회에 성공했습니다.", HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> chargeFromAccount(RequestDto.Charge charge, String loginId) {

        Optional<User> userInfo = usersRepository.findByLoginId(loginId);

        if (userInfo.isEmpty()) {
            return response.fail("유저 Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<Account> account = accountRepository.findByUserId(userInfo.get().getIdx());

        if (account.isEmpty()) {
            return response.fail("연결된 계좌가 없습니다.", HttpStatus.BAD_REQUEST);
        }

        MyWallet wallet = userInfo.get().getMyWallet();

        // 1. 남은 잔액보다 돈이 적을 때
        if (account.get().getBalance() < charge.getAmount()) {
            return response.fail("계좌 잔액이 부족합니다.", HttpStatus.BAD_REQUEST);
        }

        // 2. 남은 잔액보다 돈이 많을 때
        // 2-1. wallet 잔액 추가
        wallet.setBalance(wallet.getBalance() + charge.getAmount());
        walletRepository.save(wallet);
        // 2-2. account 잔액 차감
        account.get().setBalance(account.get().getBalance() - charge.getAmount());
        accountRepository.save(account.get());
        // 2-3. transaction 추가
        makeTransaction(wallet, charge.getAmount());
        return response.success("충전을 완료했습니다.");

    }

    public ResponseEntity<?> connectAccount(RequestDto.AccountInfo accountInfo, String loginId) {
        Optional<User> userInfo = usersRepository.findByLoginId(loginId);

        if (userInfo.isEmpty()) {
            return response.fail("유저 Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<Account> account = accountRepository.findByUserId(userInfo.get().getIdx());

        if (account.isPresent()) {
            return response.fail("연결된 계좌가 존재합니다.", HttpStatus.BAD_REQUEST);
        }

        // 연결된 계좌가 없을 때, 새로 생성 (100만원이 들어있다고 가정)
        Account newAccount = Account.builder()
                .userId(userInfo.get().getIdx())
                .accountId(accountInfo.getAccountId())
                .openDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .name(accountInfo.getName())
                .balance(1000000)
                .build();
        accountRepository.save(newAccount);
        return response.success("계좌 연동에 성공했습니다.");
    }

    public void makeTransaction(MyWallet wallet, Integer amount) {
        Transactions transactions = Transactions.builder()
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType("계좌 충전")
                .amount(amount)
                .balance(wallet.getBalance())
                .build();

        transactions.setWallet(wallet);
        transactionRepository.save(transactions);
    }

    public HashMap<String, Timestamp> getDate(Integer year, Integer month) {

        HashMap<String, Timestamp> map = new HashMap<String, Timestamp>();

        Calendar startDate = Calendar.getInstance();  // 현재 시간정보 가지고오기
        startDate.set(Calendar.YEAR, year);  //년 설정
        startDate.set(Calendar.MONTH, month - 1);  //월 설정
        startDate.set(Calendar.DATE, 1);  //일 설정
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        Calendar endDate = Calendar.getInstance();  // 현재 시간정보 가지고오기
        endDate.set(Calendar.YEAR, year);  //년 설정
        endDate.set(Calendar.MONTH, month - 1);  //월 설정
        endDate.set(Calendar.DATE, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 249);

        Timestamp start = new Timestamp(startDate.getTimeInMillis());
        Timestamp end = new Timestamp(endDate.getTimeInMillis());

        map.put("startDate", start);
        map.put("endDate", end);

        return map;
    }

    /**
     * <p>회원가입 시 생성된 유저를 바탕으로, 개인지갑 생성</p>
     * <p>이미 개인 지갑이 존재하는 경우 false</p>
     *
     * @param : 유저
     * @return : 생성 성공, 실패
     */
    public Boolean makeMyWallet(User user) {

        MyWallet myWallet = user.getMyWallet();
        if (!ObjectUtils.isEmpty(myWallet)) {
            return false;
        }

        MyWallet newWallet = MyWallet.builder()
                .balance(0)
                .build();

        //walletRepository.save(newWallet);
        user.setMyWallet(newWallet);

        return true;
    }

    public List<Transactions> getTransactionsByWallet(MyWallet myWallet) {
        List<Transactions> transactions = transactionRepository.findAllByWalletId(myWallet.getId());
        return transactions;
    }

    @Transactional
    public boolean transfer(MyWallet sendWallet, MyWallet receiveWallet, int amount, String senderComment, String receiveComment) {
        if (sendWallet.getBalance() < amount) {
            System.out.println("보내는 사람의 지갑 잔액이 부족합니다.");
            return false;
        }

        // 1. sendWallet 잔액 차감
        sendWallet.setBalance(sendWallet.getBalance() - amount);
        walletRepository.save(sendWallet);
        // 2. receiverWallet 잔액 추가
        receiveWallet.setBalance(receiveWallet.getBalance() + amount);
        walletRepository.save(receiveWallet);

        // 3. transaction 생성
        Transactions sendTransaction = makeTransaction(sendWallet, receiveWallet, amount, "출금", senderComment);
        Transactions receiveTransaction = makeTransaction(receiveWallet, sendWallet, amount, "입금", receiveComment);

        transactionRepository.save(sendTransaction);
        transactionRepository.save(receiveTransaction);

        return true;
    }

    public ResponseEntity<?> transfer(RequestDto.Transfer transfer) {
        Optional<MyWallet> sendWallet = walletRepository.findById(transfer.getSendWalletId());
        Optional<MyWallet> receiveWallet = walletRepository.findById(transfer.getReceiveWalletId());

        if (sendWallet.isEmpty()) {
            return response.fail("보내는 지갑Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        if (receiveWallet.isEmpty()) {
            return response.fail("받는 지갑Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        if (transfer.getAmount()==null) {
            return response.fail("금액은 반드시 입력해야합니다.", HttpStatus.BAD_REQUEST);
        }
        String sendMsg = transfer.getMessage();
        String receiveMsg = transfer.getMessage();
        if (transfer.getMessage() == null) {
            if (sendWallet.get().getDecriminatorValue().equals("my")) {
                receiveMsg = usersRepository.findByMyWallet(sendWallet.get()).get().getName();
            } else {
                receiveMsg = ((MoimWallet) sendWallet.get()).getWalletName();
            }
            //동일
            if (receiveWallet.get().getDecriminatorValue().equals("my")) {
                sendMsg = usersRepository.findByMyWallet(receiveWallet.get()).get().getName();
            } else {
                sendMsg = ((MoimWallet) receiveWallet.get()).getWalletName();
            }
        }
        boolean result = transfer(sendWallet.get(), receiveWallet.get(), transfer.getAmount(), sendMsg, receiveMsg);

        if (!result) {
            return response.fail("보내는 사람의 지갑 잔액이 부족합니다.", HttpStatus.BAD_REQUEST);
        }

        return response.success("이체를 성공했습니다.");
    }

    private static Transactions makeTransaction(MyWallet sendWallet, MyWallet receiveWallet, int amount, String type, String message) {
        Transactions sendTransaction = Transactions.builder()
                .wallet(sendWallet)
                .counterId(receiveWallet.getId())
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType(type)
                .amount(amount)
                .balance(sendWallet.getBalance())
                .message(message)
                .build();
        return sendTransaction;
    }
}
